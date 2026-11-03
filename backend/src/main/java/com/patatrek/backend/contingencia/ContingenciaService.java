package com.patatrek.backend.contingencia;

import com.patatrek.backend.contingencia.dto.ContingenciaRequest;
import com.patatrek.backend.contingencia.dto.ContingenciaResponse;
import com.patatrek.backend.excepcion.DatosInvalidosException;
import com.patatrek.backend.excepcion.ReglaNegocioException;
import com.patatrek.backend.notificacion.Correo;
import com.patatrek.backend.notificacion.FormatoCorreo;
import com.patatrek.backend.notificacion.NotificacionService;
import com.patatrek.backend.reserva.*;
import com.patatrek.backend.salida.*;
import com.patatrek.backend.usuario.Guia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * CU-14 Registrar contingencia de salida (RF-12). Una resolución por registro:
 *  - Cancelación: la salida pasa a Cancelada; sus reservas vigentes, a Pendiente de devolución (después se reintegran
 *    con CU-27); se liberan guías, plazas de refugio y equipamiento; correo a cada titular y participante.
 *  - Reprogramación: nueva fecha/horario (y opcionalmente recorrido). Solo si los guías y refugios asignados están
 *    libres en el nuevo período (flujo 4b). Las reservas quedan esperando que el cliente acepte o rechace (RF-12).
 *  - Modificación de recorrido: cambia el recorrido; las reservas siguen igual, sin derecho a devolución.
 */
@Service
public class ContingenciaService {

    private static final Logger log = LoggerFactory.getLogger(ContingenciaService.class);
    private static final int MAXIMO_TEXTO = 500;
    private static final int MAXIMO_RECORRIDO = 1000;

    private final ContingenciaRepository contingenciaRepository;
    private final GestionSalidaService gestionSalida;
    private final ReservaService reservaService;
    private final ParticipanteRepository participanteRepository;
    private final AsignacionGuiaRepository asignacionGuiaRepository;
    private final NotificacionService notificacionService;

    public ContingenciaService(ContingenciaRepository contingenciaRepository, GestionSalidaService gestionSalida,
                               ReservaService reservaService, ParticipanteRepository participanteRepository,
                               AsignacionGuiaRepository asignacionGuiaRepository,
                               NotificacionService notificacionService) {
        this.contingenciaRepository = contingenciaRepository;
        this.gestionSalida = gestionSalida;
        this.reservaService = reservaService;
        this.participanteRepository = participanteRepository;
        this.asignacionGuiaRepository = asignacionGuiaRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public ContingenciaResponse registrar(Long idSalida, ContingenciaRequest d) {
        Salida s = gestionSalida.buscar(idSalida, true);
        if (!GestionSalidaService.ACTIVAS.contains(s.getEstadoSalida())) {
            throw new ReglaNegocioException(
                    "Solo se puede registrar una contingencia en salidas Disponibles, Completas o En curso.");
        }

        // Paso 4 / 4a: datos obligatorios
        Map<String, String> errores = new LinkedHashMap<>();
        if (d.tipoContingencia() == null) errores.put("tipoContingencia", "Elegí el tipo de contingencia.");
        if (d.resolucion() == null) errores.put("resolucion", "Elegí la resolución.");
        String motivo = d.motivo() == null ? "" : d.motivo().trim();
        if (motivo.isEmpty()) {
            errores.put("motivo", "Ingresá el motivo de la contingencia.");
        } else if (motivo.length() > MAXIMO_TEXTO) {
            errores.put("motivo", "El motivo puede tener hasta " + MAXIMO_TEXTO + " caracteres.");
        }
        String recorrido = d.nuevoRecorrido() == null ? "" : d.nuevoRecorrido().trim();
        if (recorrido.length() > MAXIMO_RECORRIDO) {
            errores.put("nuevoRecorrido", "El recorrido puede tener hasta " + MAXIMO_RECORRIDO + " caracteres.");
        }
        if (d.resolucion() == TipoResolucion.MODIFICACION && recorrido.isEmpty()) {
            errores.put("nuevoRecorrido", "Ingresá el recorrido alternativo.");
        }
        if (!errores.isEmpty()) {
            throw new DatosInvalidosException("Revisá los datos marcados.", errores);
        }

        Contingencia c = new Contingencia();
        c.setSalida(s);
        c.setTipoContingencia(d.tipoContingencia().name());
        c.setTipoResolucion(d.resolucion().name());
        c.setMotivo(motivo);

        Resultado r = switch (d.resolucion()) {
            case CANCELACION -> cancelar(s, d.tipoContingencia(), motivo);
            case REPROGRAMACION -> reprogramar(s, c, d, recorrido, motivo);
            case MODIFICACION -> modificarRecorrido(s, c, recorrido, motivo);
        };
        contingenciaRepository.save(c);
        log.info("Contingencia {} ({}) en la salida #{}: {} reservas afectadas", d.resolucion(), d.tipoContingencia(),
                idSalida, r.reservas());
        return new ContingenciaResponse(c.getIdContingencia(), r.mensaje(), r.reservas(), r.correos(),
                gestionSalida.detalle(idSalida));
    }

    private record Resultado(String mensaje, int reservas, int correos) {}

    // =====================================================================
    //  Cancelación
    // =====================================================================

    private Resultado cancelar(Salida s, TipoContingencia tipo, String motivo) {
        String motivoReserva = "Cancelación de la salida por contingencia " + tipo.texto() + ": " + motivo;
        int afectadas = 0;
        int correos = 0;
        for (Reserva r : gestionSalida.reservasQueOcupan(s.getIdSalida())) {
            if (r.getEstadoReserva() == EstadoReserva.PENDIENTE_PAGO) {
                // Todavía no pagó: se cancela y se liberan los recursos retenidos (no hay nada que devolver)
                reservaService.cancelarPendientePorContingencia(r, motivoReserva);
                continue;
            }
            reservaService.liberarRecursosConsolidados(r, s);
            r.setEstadoReserva(EstadoReserva.PENDIENTE_DEVOLUCION);
            r.setReprogramacionPendiente(false);
            r.setMotivoCancelacion(motivoReserva);
            afectadas++;
            correos += avisar(r, destinatario -> Correo.para(destinatario)
                    .asunto("PataTrek — Se canceló tu salida de " + s.getExcursion().getNombre())
                    .titulo("Tu salida fue cancelada")
                    .parrafo("Por una contingencia " + tipo.texto() + " tuvimos que cancelar la salida. "
                            + "Lamentamos los inconvenientes.")
                    .dato("Excursión", s.getExcursion().getNombre())
                    .dato("Salida", FormatoCorreo.salida(s.getFechaInicio(), s.getFechaFinalizacion(), s.getHoraInicio()))
                    .dato("Reserva", "#" + r.getIdReserva())
                    .dato("Motivo", motivo)
                    .nota("Ya iniciamos el trámite de devolución total de lo abonado. Cuando se procese, el titular "
                            + "de la reserva recibirá el comprobante por correo."));
        }
        s.setEstadoSalida(EstadoSalida.CANCELADA);
        s.setMotivoCancelacion("Contingencia " + tipo.texto() + ": " + motivo);
        gestionSalida.liberarGuiasYRefugios(s);
        String mensaje = "La salida quedó Cancelada. "
                + (afectadas == 0 ? "No tenía reservas vigentes."
                : afectadas == 1 ? "1 reserva pasó a Pendiente de devolución y se avisó por correo."
                : afectadas + " reservas pasaron a Pendiente de devolución y se avisó por correo.");
        return new Resultado(mensaje, afectadas, correos);
    }

    // =====================================================================
    //  Reprogramación
    // =====================================================================

    private Resultado reprogramar(Salida s, Contingencia c, ContingenciaRequest d, String recorrido, String motivo) {
        if (s.getEstadoSalida() == EstadoSalida.EN_CURSO) {
            throw new ReglaNegocioException(
                    "La salida ya está en curso: solo se puede cancelar o modificar el recorrido.");
        }
        Map<String, String> errores = new LinkedHashMap<>();
        if (d.nuevaFechaInicio() == null) errores.put("nuevaFechaInicio", "Ingresá la nueva fecha de inicio.");
        if (d.nuevaHoraInicio() == null) errores.put("nuevaHoraInicio", "Ingresá el nuevo horario de inicio.");
        if (!errores.isEmpty()) throw new DatosInvalidosException("Revisá los datos marcados.", errores);

        long dias = ChronoUnit.DAYS.between(s.getFechaInicio(), s.getFechaFinalizacion());
        LocalDate nuevaFin = d.nuevaFechaInicio().plusDays(dias);
        LocalTime nuevaHoraFin = d.nuevaHoraFinalizacion() != null ? d.nuevaHoraFinalizacion() : s.getHoraFinalizacion();
        LocalDateTime desde = d.nuevaFechaInicio().atTime(d.nuevaHoraInicio());
        LocalDateTime hasta = nuevaFin.atTime(nuevaHoraFin);

        if (!desde.isAfter(LocalDateTime.now())) {
            errores.put("nuevaFechaInicio", "La nueva fecha y hora tienen que ser posteriores a la fecha y hora actual.");
        } else if (!hasta.isAfter(desde)) {
            errores.put("nuevaHoraFinalizacion", "La hora de finalización tiene que ser posterior a la de inicio.");
        } else if (desde.equals(GestionSalidaService.inicio(s)) && hasta.equals(GestionSalidaService.fin(s))) {
            errores.put("nuevaFechaInicio", "La nueva fecha y horario son iguales a los actuales.");
        }
        if (!errores.isEmpty()) throw new DatosInvalidosException("Revisá los datos marcados.", errores);

        // 4b: los guías y refugios asignados tienen que estar libres en el nuevo período
        for (AsignacionGuia a : asignacionGuiaRepository.findBySalidaIdSalidaIn(List.of(s.getIdSalida()))) {
            Guia g = a.getGuia();
            String ocupado = gestionSalida.motivoGuiaNoDisponible(g, desde, hasta, s.getIdSalida());
            if (ocupado != null) {
                throw ReglaNegocioException.enCampo("nuevaFechaInicio",
                        "No hay disponibilidad en esa fecha: " + ocupado + " Elegí otra fecha.");
            }
        }
        for (AsignacionRefugio a : gestionSalida.refugiosDe(s)) {
            int libres = gestionSalida.plazasLibres(a.getRefugio(), d.nuevaFechaInicio(), nuevaFin, s.getIdSalida());
            if (a.getPlazasReservadas() > libres) {
                throw ReglaNegocioException.enCampo("nuevaFechaInicio", "No hay disponibilidad en esa fecha: "
                        + a.getRefugio().getNombre() + " tiene " + libres + " plazas libres y la salida necesita "
                        + a.getPlazasReservadas() + ". Elegí otra fecha.");
            }
        }

        String anterior = FormatoCorreo.salida(s.getFechaInicio(), s.getFechaFinalizacion(), s.getHoraInicio());
        c.setFechaAnteriorSalida(GestionSalidaService.inicio(s));
        s.setFechaInicio(d.nuevaFechaInicio());
        s.setFechaFinalizacion(nuevaFin);
        s.setHoraInicio(d.nuevaHoraInicio());
        s.setHoraFinalizacion(nuevaHoraFin);
        if (!recorrido.isEmpty()) {
            s.setRecorrido(recorrido);
            c.setRecorridoAlternativo(recorrido);
        }
        String nueva = FormatoCorreo.salida(s.getFechaInicio(), s.getFechaFinalizacion(), s.getHoraInicio());

        int afectadas = 0;
        int correos = 0;
        for (Reserva r : gestionSalida.reservasQueOcupan(s.getIdSalida())) {
            if (!EstadoReserva.VIGENTES.contains(r.getEstadoReserva())) continue;
            r.setReprogramacionPendiente(true);
            afectadas++;
            correos += avisar(r, destinatario -> {
                boolean titular = destinatario.equalsIgnoreCase(r.getCliente().getCorreoElectronico());
                Correo.Builder b = Correo.para(destinatario)
                        .asunto("PataTrek — Cambió la fecha de tu salida de " + s.getExcursion().getNombre())
                        .titulo("Tu salida fue reprogramada")
                        .parrafo("Por una contingencia " + TipoContingencia.valueOf(c.getTipoContingencia()).texto()
                                + " tuvimos que cambiar la fecha de la salida.")
                        .dato("Excursión", s.getExcursion().getNombre())
                        .dato("Fecha anterior", anterior)
                        .datoDestacado("Nueva fecha", nueva)
                        .dato("Reserva", "#" + r.getIdReserva())
                        .dato("Motivo", motivo);
                if (!recorrido.isEmpty()) b.dato("Nuevo recorrido", recorrido);
                if (titular) {
                    b.parrafo("Entrá a tu reserva para aceptar la nueva fecha o rechazarla. Si la rechazás, te "
                                    + "devolvemos el total de lo abonado.")
                            .boton("Aceptar o rechazar la nueva fecha",
                                    notificacionService.enlace("/reservas/" + r.getIdReserva()));
                } else {
                    b.nota("El titular de la reserva va a confirmar si acepta la nueva fecha.");
                }
                return b;
            });
        }
        String mensaje = "La salida se reprogramó para el " + nueva + ". "
                + (afectadas == 0 ? "No tenía reservas vigentes."
                : "Se avisó por correo a " + (afectadas == 1 ? "1 reserva" : afectadas + " reservas")
                + ", que ahora pueden aceptar o rechazar la nueva fecha.");
        return new Resultado(mensaje, afectadas, correos);
    }

    // =====================================================================
    //  Modificación de recorrido
    // =====================================================================

    private Resultado modificarRecorrido(Salida s, Contingencia c, String recorrido, String motivo) {
        s.setRecorrido(recorrido);
        c.setRecorridoAlternativo(recorrido);
        int afectadas = 0;
        int correos = 0;
        for (Reserva r : gestionSalida.reservasQueOcupan(s.getIdSalida())) {
            if (!EstadoReserva.VIGENTES.contains(r.getEstadoReserva())) continue;
            afectadas++;
            correos += avisar(r, destinatario -> Correo.para(destinatario)
                    .asunto("PataTrek — Nuevo recorrido para tu salida de " + s.getExcursion().getNombre())
                    .titulo("Cambió el recorrido de tu salida")
                    .parrafo("Por una contingencia " + TipoContingencia.valueOf(c.getTipoContingencia()).texto()
                            + " ajustamos el recorrido. La fecha y el horario no cambian.")
                    .dato("Excursión", s.getExcursion().getNombre())
                    .dato("Salida", FormatoCorreo.salida(s.getFechaInicio(), s.getFechaFinalizacion(), s.getHoraInicio()))
                    .dato("Reserva", "#" + r.getIdReserva())
                    .dato("Motivo", motivo)
                    .datoDestacado("Nuevo recorrido", recorrido)
                    .nota("Tu reserva sigue vigente. Este cambio no modifica el cronograma, por eso no da derecho "
                            + "a devolución."));
        }
        String mensaje = "Se actualizó el recorrido. "
                + (afectadas == 0 ? "No tenía reservas vigentes."
                : "Se avisó por correo a " + (afectadas == 1 ? "1 reserva." : afectadas + " reservas."));
        return new Resultado(mensaje, afectadas, correos);
    }

    // =====================================================================

    /** Manda el correo al titular y a cada participante con e-mail (sin repetir direcciones). */
    private int avisar(Reserva r, java.util.function.Function<String, Correo.Builder> armar) {
        Map<String, String> destinatarios = new LinkedHashMap<>(); // correo → nombre para el saludo
        destinatarios.put(r.getCliente().getCorreoElectronico().trim().toLowerCase(Locale.ROOT), r.getCliente().getNombre());
        for (Participante p : participanteRepository.findByReservaIdReservaOrderByIdParticipanteAsc(r.getIdReserva())) {
            if (p.getCorreoElectronico() != null && !p.getCorreoElectronico().isBlank()) {
                destinatarios.putIfAbsent(p.getCorreoElectronico().trim().toLowerCase(Locale.ROOT), p.getNombre());
            }
        }
        destinatarios.forEach((correo, nombre) ->
                notificacionService.enviar(armar.apply(correo).saludo("Hola " + nombre + ":").build()));
        return destinatarios.size();
    }
}
