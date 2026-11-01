package com.patatrek.backend.certificado;

import com.patatrek.backend.archivo.servicio.ArchivoService;
import com.patatrek.backend.certificado.dto.CertificadoParticipanteResponse;
import com.patatrek.backend.certificado.dto.CertificadoValidacionResponse;
import com.patatrek.backend.certificado.dto.CertificadosReservaResponse;
import com.patatrek.backend.excepcion.AccesoDenegadoException;
import com.patatrek.backend.excepcion.RecursoNoEncontradoException;
import com.patatrek.backend.excepcion.ReglaNegocioException;
import com.patatrek.backend.notificacion.NotificacionService;
import com.patatrek.backend.reserva.EstadoReserva;
import com.patatrek.backend.reserva.EstadoRequisitos;
import com.patatrek.backend.reserva.ModalidadAlojamiento;
import com.patatrek.backend.reserva.Participante;
import com.patatrek.backend.reserva.ParticipanteRepository;
import com.patatrek.backend.reserva.Reserva;
import com.patatrek.backend.reserva.ReservaRepository;
import com.patatrek.backend.salida.AsignacionRefugio;
import com.patatrek.backend.salida.AsignacionRefugioRepository;
import com.patatrek.backend.salida.EstadoSalida;
import com.patatrek.backend.salida.Salida;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CU-24 Cargar certificado médico (Cliente / Participante) y CU-25 Validar certificados médicos (Administrador).
 */
@Service
public class CertificadoMedicoService {

    /** Los certificados se cargan hasta 72 hs antes del inicio de la salida (RN del CU-24). */
    static final Duration PLAZO_LIMITE = Duration.ofHours(72);

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");

    private final CertificadoMedicoRepository certificadoRepository;
    private final ArchivoService archivoService;
    private final ReservaRepository reservaRepository;
    private final ParticipanteRepository participanteRepository;
    private final AsignacionRefugioRepository asignacionRefugioRepository;
    private final NotificacionService notificacionService;

    public CertificadoMedicoService(CertificadoMedicoRepository certificadoRepository,
                                    ArchivoService archivoService,
                                    ReservaRepository reservaRepository,
                                    ParticipanteRepository participanteRepository,
                                    AsignacionRefugioRepository asignacionRefugioRepository,
                                    NotificacionService notificacionService) {
        this.certificadoRepository = certificadoRepository;
        this.archivoService = archivoService;
        this.reservaRepository = reservaRepository;
        this.participanteRepository = participanteRepository;
        this.asignacionRefugioRepository = asignacionRefugioRepository;
        this.notificacionService = notificacionService;
    }

    // =====================================================================================
    //  CU-24 Cargar certificado médico
    // =====================================================================================

    /**
     * Crea el certificado en estado PENDIENTE de cada participante de la reserva, si la excursión exige certificado.
     * Se invoca al registrar la reserva (CU-18), en la misma transacción y con los participantes ya guardados.
     * No hace nada si la excursión no lo requiere, y no duplica los certificados que ya existan.
     */
    @Transactional
    public void crearPendientes(Reserva reserva) {
        if (!reserva.getSalida().getExcursion().isRequiereCertificado()) {
            return;
        }
        List<Participante> participantes =
                participanteRepository.findByReservaIdReservaOrderByApellidoAscNombreAsc(reserva.getIdReserva());
        if (participantes.isEmpty()) {
            return;
        }
        List<Long> ids = participantes.stream().map(Participante::getIdParticipante).toList();
        Set<Long> conCertificado = certificadoRepository.findByParticipanteIdParticipanteIn(ids).stream()
                .map(c -> c.getParticipante().getIdParticipante())
                .collect(Collectors.toSet());
        for (Participante p : participantes) {
            if (!conCertificado.contains(p.getIdParticipante())) {
                crearPendiente(p);
            }
        }
    }

    /**
     * Certificados de una reserva. Sin dni (Cliente): todos los participantes. Con dni (Participante): solo el suyo.
     * Los certificados PENDIENTE se crean al registrar la reserva (crearPendientes). Acá se vuelve a invocar solo como
     * respaldo para reservas anteriores a ese cambio; cuando todas estén migradas puede quitarse y pasar a solo lectura.
     */
    @Transactional
    public CertificadosReservaResponse certificadosDeReserva(Long idReserva, String dni) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva #" + idReserva + "."));
        validarReservaConCertificados(reserva);

        List<Participante> participantes;
        if (dni == null || dni.isBlank()) {
            participantes = participanteRepository.findByReservaIdReservaOrderByApellidoAscNombreAsc(idReserva);
        } else {
            // Acceso del participante: ID de reserva + DNI (CU-19). Si no coinciden, no se revela nada más.
            participantes = participanteRepository.findByReservaIdReservaAndDni(idReserva, dni.trim())
                    .map(List::of)
                    .orElseThrow(() -> new AccesoDenegadoException("Los datos ingresados no son correctos."));
        }

        crearPendientes(reserva); // respaldo (ver arriba)

        List<Long> ids = participantes.stream().map(Participante::getIdParticipante).toList();
        Map<Long, CertificadoMedico> existentes = new HashMap<>();
        for (CertificadoMedico c : certificadoRepository.findByParticipanteIdParticipanteIn(ids)) {
            existentes.put(c.getParticipante().getIdParticipante(), c);
        }

        String dniTitular = reserva.getCliente().getDni();
        List<CertificadoParticipanteResponse> filas = new ArrayList<>();
        for (Participante p : participantes) {
            CertificadoMedico c = existentes.get(p.getIdParticipante());
            filas.add(CertificadoParticipanteResponse.de(c, p, p.getDni().equals(dniTitular)));
        }
        return new CertificadosReservaResponse(horarioLimite(reserva.getSalida()), filas);
    }

    /**
     * Guarda el archivo y deja el certificado en estado CARGADO.
     * dni: lo envía el Participante; si viene, tiene que ser el del dueño del certificado.
     * Sin dni se asume al Cliente titular (hasta que el módulo de Autenticación lo identifique).
     */
    @Transactional
    public CertificadoMedico confirmarEnvio(Long idCertificado, MultipartFile archivo, String dni) {
        CertificadoMedico c = buscar(idCertificado);
        Participante participante = c.getParticipante();

        if (dni != null && !dni.isBlank() && !participante.getDni().equals(dni.trim())) {
            throw new AccesoDenegadoException("Solo podés cargar tu propio certificado.");
        }
        Reserva reserva = participante.getReserva();
        validarReservaConCertificados(reserva);

        if (!puedeCargar(c.getEstadoCertificado())) {
            throw new ReglaNegocioException("El certificado no admite una nueva carga.");
        }
        if (!LocalDateTime.now().isBefore(horarioLimite(reserva.getSalida()))) {
            throw new ReglaNegocioException(
                    "El plazo para cargar o reemplazar certificados venció (72 horas antes de la salida).");
        }
        validarArchivoCertificado(archivo);

        String pathAnterior = c.getPathArchivoAdjunto();
        c.setPathArchivoAdjunto(archivoService.guardar(archivo, "certificados"));
        c.setEstadoCertificado(EstadoCertificado.CARGADO);
        c.setFechaSubida(LocalDateTime.now());
        c.setMotivoRechazo(null); // el motivo del rechazo anterior ya no aplica a este nuevo envío
        c.incrementarIntentos();
        CertificadoMedico guardado = certificadoRepository.save(c);

        if (pathAnterior != null) {
            archivoService.eliminar(pathAnterior); // el archivo rechazado ya no sirve
        }
        return guardado;
    }

    // =====================================================================================
    //  CU-25 Validar certificados médicos
    // =====================================================================================

    @Transactional(readOnly = true)
    public List<CertificadoValidacionResponse> listarCertificadosAValidar() {
        return certificadoRepository.buscarParaValidar(
                        EstadoCertificado.CARGADO,
                        EstadoReserva.VIGENTES,
                        EnumSet.of(EstadoSalida.DISPONIBLE, EstadoSalida.COMPLETA))
                .stream()
                .map(c -> CertificadoValidacionResponse.de(c, nombreArchivo(c)))
                .toList();
    }

    /** Archivo adjunto para que el administrador lo abra, con el nombre con el que se muestra. */
    public record ArchivoCertificado(Path path, String nombre) {
    }

    @Transactional(readOnly = true)
    public ArchivoCertificado obtenerArchivo(Long idCertificado) {
        CertificadoMedico c = buscar(idCertificado);
        if (c.getPathArchivoAdjunto() == null) {
            throw new ReglaNegocioException("El certificado no tiene archivo adjunto.");
        }
        Path path = archivoService.obtener(c.getPathArchivoAdjunto());
        if (!Files.exists(path)) {
            throw new RecursoNoEncontradoException("El archivo del certificado no está disponible.");
        }
        return new ArchivoCertificado(path, nombreArchivo(c));
    }

    @Transactional
    public CertificadoMedico aprobar(Long idCertificado) {
        CertificadoMedico c = buscar(idCertificado);
        exigirCargado(c);
        c.setEstadoCertificado(EstadoCertificado.APROBADO);
        CertificadoMedico guardado = certificadoRepository.save(c);

        Participante p = c.getParticipante();
        notificarAlCliente(p.getReserva(), "Certificado médico aprobado",
                "El certificado médico de " + p.getNombreCompleto() + " fue aprobado para la excursión "
                        + p.getReserva().getSalida().getExcursion().getNombre() + ".\n\n"
                        + "Recordá que el día de la salida el guía hará el control de asistencia y equipamiento.");
        return guardado;
    }

    /**
     * Primer rechazo: RECHAZADO (puede volver a cargar). Segundo rechazo: RECHAZADO_DEFINITIVO, el participante
     * queda No apto y se liberan su cupo y su plaza de refugio. No hay devolución (regla del CU-25).
     */
    @Transactional
    public CertificadoMedico rechazar(Long idCertificado, String motivo) {
        CertificadoMedico c = buscar(idCertificado);
        exigirCargado(c);
        c.setMotivoRechazo(motivo.trim());

        Participante p = c.getParticipante();
        Reserva reserva = p.getReserva();
        String cuerpo;
        if (c.alcanzoMaximoIntentos()) {
            c.setEstadoCertificado(EstadoCertificado.RECHAZADO_DEFINITIVO);
            liberarRecursos(p);
            cuerpo = "El certificado médico de " + p.getNombreCompleto() + " fue rechazado de forma definitiva.\n"
                    + "Motivo: " + c.getMotivoRechazo() + "\n\n"
                    + "Ya no es posible cargar otro certificado: se liberó su lugar en la salida. "
                    + "Este rechazo no da derecho a devolución.";
        } else {
            c.setEstadoCertificado(EstadoCertificado.RECHAZADO);
            cuerpo = "El certificado médico de " + p.getNombreCompleto() + " fue rechazado.\n"
                    + "Motivo: " + c.getMotivoRechazo() + "\n\n"
                    + "Podés volver a cargarlo una vez más hasta el "
                    + horarioLimite(reserva.getSalida()).format(FECHA_HORA) + ".";
        }
        CertificadoMedico guardado = certificadoRepository.save(c);
        notificarAlCliente(reserva, "Certificado médico rechazado", cuerpo);
        return guardado;
    }

    // =====================================================================================
    //  Auxiliares
    // =====================================================================================

    private CertificadoMedico buscar(Long id) {
        return certificadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el certificado médico #" + id + "."));
    }

    private CertificadoMedico crearPendiente(Participante p) {
        CertificadoMedico c = new CertificadoMedico();
        c.setParticipante(p);
        c.setEstadoCertificado(EstadoCertificado.PENDIENTE);
        c.setFechaSubida(null); // todavía no se subió nada (el constructor la inicializa con "ahora")
        return certificadoRepository.save(c);
    }

    //falta que la salida este valida (no se puede acceder antes de las 72hs)
    /** Solo las reservas Señadas o Confirmadas de excursiones que exigen certificado trabajan con certificados. */
    private void validarReservaConCertificados(Reserva reserva) {
        if (!EstadoReserva.VIGENTES.contains(reserva.getEstadoReserva())) {
            throw new ReglaNegocioException("La reserva no admite certificados médicos: debe estar Señada o Confirmada.");
        }
        if (!reserva.getSalida().getExcursion().isRequiereCertificado()) {
            throw new ReglaNegocioException("Esta excursión no requiere certificado médico.");
        }
    }

    private boolean puedeCargar(EstadoCertificado estado) {
        return switch (estado) {
            case PENDIENTE, RECHAZADO -> true;
            case CARGADO, APROBADO, RECHAZADO_DEFINITIVO -> false;
        };
    }

    private void exigirCargado(CertificadoMedico c) {
        if (c.getEstadoCertificado() != EstadoCertificado.CARGADO) {
            throw new ReglaNegocioException("Solo se pueden validar certificados en estado Cargado.");
        }
    }

    private void validarArchivoCertificado(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("No se subió ningún archivo.");
        }
        if (archivo.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido (5 MB).");
        }
        String tipo = archivo.getContentType();
        if (!List.of("application/pdf", "image/jpeg", "image/png").contains(tipo)) {
            throw new IllegalArgumentException("El archivo no es del tipo requerido (PDF, JPG o PNG).");
        }
    }

    /** 72 hs antes del inicio de la salida. */
    static LocalDateTime horarioLimite(Salida salida) {
        return LocalDateTime.of(salida.getFechaInicio(), salida.getHoraInicio()).minus(PLAZO_LIMITE);
    }

    /** Rechazo definitivo: el participante queda No apto y se libera su lugar (cupo y plaza de refugio). */
    private void liberarRecursos(Participante p) {
        p.setEstadoRequisitos(EstadoRequisitos.NO_APTO);

        Reserva reserva = p.getReserva();
        Salida salida = reserva.getSalida();

        salida.setCuposOcupados(Math.max(0, salida.getCuposOcupados() - 1));
        // La reserva deja de ocupar ese cupo: así, si luego se cancela, no se libera dos veces
        reserva.setCantidadParticipantes(Math.max(0, reserva.getCantidadParticipantes() - 1));
        if (salida.getEstadoSalida() == EstadoSalida.COMPLETA && salida.getCuposDisponibles() > 0) {
            salida.setEstadoSalida(EstadoSalida.DISPONIBLE);
        }

        if (reserva.getModalidadAlojamiento() == ModalidadAlojamiento.REFUGIO) {
            for (AsignacionRefugio ar : asignacionRefugioRepository
                    .findBySalidaIdSalidaInOrderByOrdenVisitaAsc(List.of(salida.getIdSalida()))) {
                ar.setPlazasOcupadas(Math.max(0, ar.getPlazasOcupadas() - 1));
            }
        }
    }

    /** Mail al cliente titular. NotificacionService nunca interrumpe la operación si el envío falla. */
    private void notificarAlCliente(Reserva reserva, String asunto, String cuerpo) {
        notificacionService.enviar(reserva.getCliente().getCorreoElectronico(), asunto,
                "Hola " + reserva.getCliente().getNombre() + ",\n\n" + cuerpo + "\n\nPataTrek");
    }

    /** Nombre con el que se muestra el archivo, por ejemplo "certificado_ibarra.pdf". */
    private String nombreArchivo(CertificadoMedico c) {
        String apellido = Normalizer.normalize(c.getParticipante().getApellido(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "_");
        String path = c.getPathArchivoAdjunto();
        String extension = path != null && path.contains(".") ? path.substring(path.lastIndexOf('.')) : "";
        return "certificado_" + apellido + extension;
    }
}