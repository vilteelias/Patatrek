package com.patatrek.backend.contingencia.estrategias;

import com.patatrek.backend.contingencia.*;
import com.patatrek.backend.contingencia.dto.ContingenciaRequest;
import com.patatrek.backend.excepcion.DatosInvalidosException;
import com.patatrek.backend.excepcion.ReglaNegocioException;
import com.patatrek.backend.notificacion.Correo;
import com.patatrek.backend.notificacion.FormatoCorreo;
import com.patatrek.backend.notificacion.NotificacionService;
import com.patatrek.backend.reserva.*;
import com.patatrek.backend.salida.*;
import com.patatrek.backend.usuario.Guia;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ReprogramacionStrategy implements ResolucionContingenciaStrategy {

    private final GestionSalidaService gestionSalida;
    private final AsignacionGuiaRepository asignacionGuiaRepository;
    private final ParticipanteRepository participanteRepository;
    private final NotificacionService notificacionService;

    public ReprogramacionStrategy(GestionSalidaService gestionSalida, AsignacionGuiaRepository asignacionGuiaRepository,
                                  ParticipanteRepository participanteRepository, NotificacionService notificacionService) {
        this.gestionSalida = gestionSalida;
        this.asignacionGuiaRepository = asignacionGuiaRepository;
        this.participanteRepository = participanteRepository;
        this.notificacionService = notificacionService;
    }

    @Override
    public TipoResolucion getTipoResolucion() {
        return TipoResolucion.REPROGRAMACION;
    }

    @Override
    public ResultadoContingencia procesar(Salida s, Contingencia c, ContingenciaRequest d) {
        if (s.getEstadoSalida() == EstadoSalida.EN_CURSO) {
            throw new ReglaNegocioException("La salida ya está en curso: solo se puede cancelar o modificar el recorrido.");
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

        for (AsignacionGuia a : asignacionGuiaRepository.findBySalidaIdSalidaIn(List.of(s.getIdSalida()))) {
            Guia g = a.getGuia();
            String ocupado = gestionSalida.motivoGuiaNoDisponible(g, desde, hasta, s.getIdSalida());
            if (ocupado != null) {
                throw ReglaNegocioException.enCampo("nuevaFechaInicio", "No hay disponibilidad en esa fecha: " + ocupado + " Elegí otra fecha.");
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
        
        String recorrido = d.nuevoRecorrido() == null ? "" : d.nuevoRecorrido().trim();
        if (!recorrido.isEmpty()) {
            s.setRecorrido(recorrido);
            c.setRecorridoAlternativo(recorrido);
        }
        String nueva = FormatoCorreo.salida(s.getFechaInicio(), s.getFechaFinalizacion(), s.getHoraInicio());

        int afectadas = 0;
        int correos = 0;
        String motivo = c.getMotivo();
        for (Reserva r : gestionSalida.reservasQueOcupan(s.getIdSalida())) {
            if (!EstadoReserva.VIGENTES.contains(r.getEstadoReserva())) continue;
            r.setReprogramacionPendiente(true);
            afectadas++;
            correos += avisar(r, s, c, anterior, nueva, recorrido, motivo, destinatario -> {
                boolean titular = destinatario.equalsIgnoreCase(r.getCliente().getCorreoElectronico());
                Correo.Builder b = Correo.para(destinatario)
                        .asunto("PataTrek — Cambió la fecha de tu salida de " + s.getExcursion().getNombre())
                        .titulo("Tu salida fue reprogramada")
                        .parrafo("Por una contingencia " + TipoContingencia.valueOf(c.getTipoContingencia()).texto() + " tuvimos que cambiar la fecha de la salida.")
                        .dato("Excursión", s.getExcursion().getNombre())
                        .dato("Fecha anterior", anterior)
                        .datoDestacado("Nueva fecha", nueva)
                        .dato("Reserva", "#" + r.getIdReserva())
                        .dato("Motivo", motivo);
                if (!recorrido.isEmpty()) b.dato("Nuevo recorrido", recorrido);
                if (titular) {
                    b.parrafo("Entrá a tu reserva para aceptar la nueva fecha o rechazarla. Si la rechazás, te devolvemos el total de lo abonado.")
                            .boton("Aceptar o rechazar la nueva fecha", notificacionService.enlace("/reservas/" + r.getIdReserva()));
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
        return new ResultadoContingencia(mensaje, afectadas, correos);
    }

    private int avisar(Reserva r, Salida s, Contingencia c, String anterior, String nueva, String recorrido, String motivo, java.util.function.Function<String, Correo.Builder> armar) {
        Map<String, String> destinatarios = new LinkedHashMap<>();
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
