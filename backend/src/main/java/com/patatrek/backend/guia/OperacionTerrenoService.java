package com.patatrek.backend.guia;

import com.patatrek.backend.auth.Rol;
import com.patatrek.backend.auth.UsuarioActual;
import com.patatrek.backend.certificado.CertificadoMedico;
import com.patatrek.backend.certificado.CertificadoMedicoRepository;
import com.patatrek.backend.certificado.EstadoCertificado;
import com.patatrek.backend.equipamiento.Equipamiento;
import com.patatrek.backend.equipamiento.EquipamientoResponse;
import com.patatrek.backend.excepcion.AccesoDenegadoException;
import com.patatrek.backend.excepcion.CredencialesInvalidasException;
import com.patatrek.backend.excepcion.RecursoNoEncontradoException;
import com.patatrek.backend.excepcion.ReglaNegocioException;
import com.patatrek.backend.excursion.Excursion;
import com.patatrek.backend.guia.dto.*;
import com.patatrek.backend.notificacion.Correo;
import com.patatrek.backend.notificacion.FormatoCorreo;
import com.patatrek.backend.notificacion.NotificacionService;
import com.patatrek.backend.reserva.*;
import com.patatrek.backend.salida.*;
import com.patatrek.backend.usuario.Guia;
import com.patatrek.backend.usuario.GuiaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Paquete "Operación en terreno" (Documento de Arquitectura):
 * CU-10 Listar salidas (vista del guía), CU-22 Listar participantes,
 * CU-15 Control de asistencia, equipamiento y certificados, CU-16 Marcar finalización de salida.
 */
@Service
public class OperacionTerrenoService {

    static final String MOTIVO_EQUIPAMIENTO = "Falta el equipamiento obligatorio mínimo.";
    static final String MOTIVO_CERTIFICADO = "El certificado médico no está en estado Aprobado.";

    private static final Set<EstadoSalida> ESTADOS_PARA_CONTROL = EnumSet.of(EstadoSalida.DISPONIBLE, EstadoSalida.COMPLETA);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final GuiaRepository guiaRepository;
    private final AsignacionGuiaRepository asignacionGuiaRepository;
    private final AsignacionRefugioRepository asignacionRefugioRepository;
    private final ParticipanteRepository participanteRepository;
    private final CertificadoMedicoRepository certificadoRepository;
    private final AsignacionEquipamientoRepository asignacionEquipamientoRepository;
    private final EntregaEquipamientoRepository entregaEquipamientoRepository;
    private final NotificacionService notificacionService;
    private final String correoGuiaDemo;
    private final String urlFrontend;
    private final UsuarioActual usuarioActual;
    private final boolean exigirLogin;

    public OperacionTerrenoService(GuiaRepository guiaRepository,
                                   AsignacionGuiaRepository asignacionGuiaRepository,
                                   AsignacionRefugioRepository asignacionRefugioRepository,
                                   ParticipanteRepository participanteRepository,
                                   CertificadoMedicoRepository certificadoRepository,
                                   AsignacionEquipamientoRepository asignacionEquipamientoRepository,
                                   EntregaEquipamientoRepository entregaEquipamientoRepository,
                                   NotificacionService notificacionService,
                                   @Value("${patatrek.guia-demo-correo:martin.almeida@patatrek.com}") String correoGuiaDemo,
                                   @Value("${patatrek.frontend-url:http://localhost:5173}") String urlFrontend,
                                   UsuarioActual usuarioActual,
                                   @Value("${patatrek.seguridad.exigir-login:false}") boolean exigirLogin) {
        this.guiaRepository = guiaRepository;
        this.asignacionGuiaRepository = asignacionGuiaRepository;
        this.asignacionRefugioRepository = asignacionRefugioRepository;
        this.participanteRepository = participanteRepository;
        this.certificadoRepository = certificadoRepository;
        this.asignacionEquipamientoRepository = asignacionEquipamientoRepository;
        this.entregaEquipamientoRepository = entregaEquipamientoRepository;
        this.notificacionService = notificacionService;
        this.correoGuiaDemo = correoGuiaDemo;
        this.urlFrontend = urlFrontend;
        this.usuarioActual = usuarioActual;
        this.exigirLogin = exigirLogin;
    }

    // =========================================================
    //  Guía actual
    // =========================================================

    /**
     * El guía que inició sesión. Si nadie inició sesión como guía y todavía no se exige login
     * (patatrek.seguridad.exigir-login=false), se usa el guía de demostración (patatrek.guia-demo-correo).
     */
    @Transactional(readOnly = true)
    public Guia guiaActual() {
        var sesion = usuarioActual.conRol(Rol.GUIA);
        if (sesion.isPresent()) {
            return guiaRepository.findById(sesion.get().id())
                    .orElseThrow(() -> new CredencialesInvalidasException("Tu sesión ya no es válida. Iniciá sesión de nuevo."));
        }
        if (exigirLogin) {
            throw new CredencialesInvalidasException("Iniciá sesión como guía para continuar.");
        }
        return guiaRepository.findByCorreoElectronicoIgnoreCase(correoGuiaDemo)
                .or(guiaRepository::findFirstByOrderByIdUsuarioAsc)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay guías cargados en el sistema."));
    }

    // =========================================================
    //  CU-10 Listar salidas (solo las asignadas al guía)
    // =========================================================

    @Transactional(readOnly = true)
    public List<SalidaGuiaResponse> misSalidas() {
        Guia guia = guiaActual();
        List<AsignacionGuia> asignaciones = asignacionGuiaRepository.findByGuiaIdUsuario(guia.getIdUsuario());
        return armarRespuestas(asignaciones).stream()
                .sorted(Comparator.comparing(SalidaGuiaResponse::fechaInicio)
                        .thenComparing(SalidaGuiaResponse::horaInicio))
                .toList();
    }

    @Transactional(readOnly = true)
    public SalidaGuiaResponse detalleSalida(Long idSalida) {
        return armarRespuestas(List.of(asignacionDelGuia(idSalida))).get(0);
    }

    // =========================================================
    //  CU-22 Listar participantes
    // =========================================================

    @Transactional(readOnly = true)
    public List<ParticipanteGuiaResponse> participantes(Long idSalida) {
        asignacionDelGuia(idSalida);
        List<Participante> nomina = nomina(idSalida);

        Map<Long, EstadoCertificado> certificados = certificadosVigentes(nomina);
        Map<Long, List<Long>> solicitados = equipamientoSolicitadoPorParticipante(nomina);

        return nomina.stream().map(p -> new ParticipanteGuiaResponse(
                p.getIdParticipante(), p.getNombre(), p.getApellido(), p.getDni(), p.getNroTelefono(),
                p.getCorreoElectronico(), p.getReserva().getIdReserva(),
                p.getEstadoAsistencia(), p.getEstadoRequisitos(),
                certificados.containsKey(p.getIdParticipante())
                        ? new ParticipanteGuiaResponse.CertificadoResumen(certificados.get(p.getIdParticipante()))
                        : null,
                solicitados.getOrDefault(p.getIdParticipante(), List.of())
        )).toList();
    }

    // =========================================================
    //  CU-15 Control de asistencia, equipamiento y certificados
    // =========================================================

    @Transactional
    public SalidaGuiaResponse registrarControl(Long idSalida, List<ControlAsistenciaRequest.RegistroAsistencia> registros) {
        AsignacionGuia asignacion = asignacionDelGuia(idSalida); // responsable o ayudante
        Salida salida = asignacion.getSalida();

        if (!ESTADOS_PARA_CONTROL.contains(salida.getEstadoSalida())) {
            throw new ReglaNegocioException(
                    "El control de asistencia solo se puede hacer antes de iniciar la salida (estado Disponible o Completa).");
        }

        List<Participante> nomina = nomina(idSalida);
        if (nomina.isEmpty()) {
            throw new ReglaNegocioException("La salida no tiene participantes para controlar.");
        }

        Map<Long, ControlAsistenciaRequest.RegistroAsistencia> porParticipante = new HashMap<>();
        for (ControlAsistenciaRequest.RegistroAsistencia r : registros) {
            porParticipante.put(r.idParticipante(), r);
        }

        Set<Long> idsNomina = nomina.stream().map(Participante::getIdParticipante).collect(Collectors.toSet());
        if (!idsNomina.containsAll(porParticipante.keySet())) {
            throw new ReglaNegocioException("Hay participantes que no pertenecen a esta salida.");
        }

        // Flujo alternativo 8a: no se puede finalizar con participantes sin asistencia registrada
        List<String> faltantes = nomina.stream()
                .filter(p -> !porParticipante.containsKey(p.getIdParticipante()))
                .map(Participante::getNombreCompleto)
                .toList();
        if (!faltantes.isEmpty()) {
            throw new ReglaNegocioException("Faltan registrar la asistencia de: " + String.join("; ", faltantes) + ".");
        }

        Excursion excursion = salida.getExcursion();
        Set<Equipamiento> obligatorio = excursion.getEquipamientoObligatorio();
        Map<Long, EstadoCertificado> certificados = certificadosVigentes(nomina);
        Map<Long, List<Long>> solicitados = equipamientoSolicitadoPorParticipante(nomina);

        List<EntregaEquipamiento> entregas = new ArrayList<>();
        for (Participante p : nomina) {
            ControlAsistenciaRequest.RegistroAsistencia registro = porParticipante.get(p.getIdParticipante());
            p.setEstadoAsistencia(registro.estadoAsistencia());

            if (registro.estadoAsistencia() == EstadoAsistencia.AUSENTE) {
                // Flujo 4a: el ausente queda fuera del cálculo de Apto / No apto
                p.setEstadoRequisitos(null);
                continue;
            }

            Set<Long> presentado = registro.equipamientoPresentado() == null ? Set.of()
                    : new HashSet<>(registro.equipamientoPresentado());
            List<Long> prestados = solicitados.getOrDefault(p.getIdParticipante(), List.of());

            boolean tieneEquipamiento = true;
            for (Equipamiento item : obligatorio) {
                boolean loTiene = presentado.contains(item.getIdEquipamiento());
                tieneEquipamiento &= loTiene;
                int cantidadPrestada = loTiene && prestados.contains(item.getIdEquipamiento()) ? 1 : 0;
                entregas.add(new EntregaEquipamiento(item, p, cantidadPrestada, loTiene));
            }

            boolean certificadoOk = !excursion.isRequiereCertificado()
                    || certificados.get(p.getIdParticipante()) == EstadoCertificado.APROBADO;

            p.setEstadoRequisitos(tieneEquipamiento && certificadoOk ? EstadoRequisitos.APTO : EstadoRequisitos.NO_APTO);
        }

        entregaEquipamientoRepository.saveAll(entregas);
        participanteRepository.saveAll(nomina);
        salida.setEstadoSalida(EstadoSalida.EN_CURSO);

        return armarRespuestas(List.of(asignacion)).get(0);
    }

    // =========================================================
    //  CU-16 Marcar finalización de salida
    // =========================================================

    @Transactional
    public FinalizacionResponse finalizar(Long idSalida, String observaciones) {
        AsignacionGuia asignacion = asignacionDelGuia(idSalida);
        if (!asignacion.esResponsable()) {
            throw new AccesoDenegadoException("Solo el guía responsable puede finalizar la salida.");
        }
        Salida salida = asignacion.getSalida();
        if (salida.getEstadoSalida() != EstadoSalida.EN_CURSO) {
            throw new ReglaNegocioException("Solo se puede finalizar una salida que esté En curso.");
        }

        String texto = observaciones == null ? null : observaciones.trim();
        salida.setObservaciones(texto == null || texto.isEmpty() ? null : texto);
        salida.setFechaHoraFinReal(LocalDateTime.now());
        salida.setEstadoSalida(EstadoSalida.FINALIZADA);

        // Solo los participantes Presente y Apto reciben la solicitud de valoración
        List<Participante> habilitados = nomina(idSalida).stream()
                .filter(p -> p.getEstadoAsistencia() == EstadoAsistencia.PRESENTE
                        && p.getEstadoRequisitos() == EstadoRequisitos.APTO)
                .toList();

        String nombreExcursion = salida.getExcursion().getNombre();
        for (Participante p : habilitados) {
            // CU-16 paso 5 / RF-14: solicitud de valoración con el enlace al formulario (CU-28)
            notificacionService.enviar(Correo.para(p.getCorreoElectronico())
                    .asunto("PataTrek — ¿Cómo estuvo tu excursión " + nombreExcursion + "?")
                    .titulo("¿Cómo estuvo tu excursión?")
                    .saludo("Hola " + p.getNombre() + ":")
                    .parrafo("Gracias por participar de " + nombreExcursion + " con PataTrek. "
                            + "Nos ayudaría mucho que nos cuentes cómo te fue: elegí de 1 a 5 estrellas y, si querés, dejanos un comentario.")
                    .dato("Excursión", nombreExcursion)
                    .dato("Salida", FormatoCorreo.salida(salida.getFechaInicio(), salida.getFechaFinalizacion(), salida.getHoraInicio()))
                    .dato("Reserva", "#" + p.getReserva().getIdReserva())
                    .boton("Valorar la excursión", urlFrontend + "/valorar?reserva=" + p.getReserva().getIdReserva() + "&dni=" + p.getDni())
                    .nota("Solo se puede registrar una valoración por participante. ¡Te esperamos en la próxima salida!")
                    .build());
        }

        return new FinalizacionResponse(habilitados.size(), armarRespuestas(List.of(asignacion)).get(0));
    }

    // =========================================================
    //  Auxiliares
    // =========================================================

    /** La salida tiene que existir y el guía actual tiene que estar asignado (responsable o ayudante). */
    private AsignacionGuia asignacionDelGuia(Long idSalida) {
        Guia guia = guiaActual();
        return asignacionGuiaRepository.findByGuiaIdUsuarioAndSalidaIdSalida(guia.getIdUsuario(), idSalida)
                .orElseThrow(() -> new AccesoDenegadoException(
                        "No estás asignado a esta salida o la salida no existe."));
    }

    private List<Participante> nomina(Long idSalida) {
        return participanteRepository.findByReservaSalidaIdSalidaAndReservaEstadoReservaInOrderByApellidoAscNombreAsc(
                idSalida, EstadoReserva.VIGENTES);
    }

    /** Estado del certificado más reciente de cada participante. */
    private Map<Long, EstadoCertificado> certificadosVigentes(List<Participante> participantes) {
        if (participantes.isEmpty()) return Map.of();
        List<Long> ids = participantes.stream().map(Participante::getIdParticipante).toList();
        Map<Long, EstadoCertificado> resultado = new HashMap<>();
        for (CertificadoMedico c : certificadoRepository.findByParticipanteIdParticipanteInOrderByFechaSubidaDesc(ids)) {
            resultado.putIfAbsent(c.getParticipante().getIdParticipante(), c.getEstadoCertificado());
        }
        return resultado;
    }

    /**
     * La reserva pide N unidades de un ítem para todo el grupo: se asignan a los primeros N participantes
     * de esa reserva (los demás llevan el suyo).
     */
    private Map<Long, List<Long>> equipamientoSolicitadoPorParticipante(List<Participante> participantes) {
        if (participantes.isEmpty()) return Map.of();
        Map<Long, List<Participante>> porReserva = participantes.stream()
                .sorted(Comparator.comparing(Participante::getIdParticipante))
                .collect(Collectors.groupingBy(p -> p.getReserva().getIdReserva(), LinkedHashMap::new, Collectors.toList()));

        Map<Long, List<Long>> resultado = new HashMap<>();
        for (AsignacionEquipamiento a : asignacionEquipamientoRepository
                .findByReservaIdReservaInAndEstadoAsignacionNot(porReserva.keySet(), EstadoAsignacion.LIBERADO)) {
            List<Participante> grupo = porReserva.getOrDefault(a.getId().getIdReserva(), List.of());
            int cantidad = Math.min(a.getCantidadSolicitada(), grupo.size());
            for (int i = 0; i < cantidad; i++) {
                resultado.computeIfAbsent(grupo.get(i).getIdParticipante(), k -> new ArrayList<>())
                        .add(a.getId().getIdEquipamiento());
            }
        }
        return resultado;
    }

    /** Arma las respuestas con los guías y refugios de cada salida. */
    private List<SalidaGuiaResponse> armarRespuestas(List<AsignacionGuia> asignaciones) {
        if (asignaciones.isEmpty()) return List.of();
        List<Long> idsSalida = asignaciones.stream().map(a -> a.getSalida().getIdSalida()).toList();

        Map<Long, List<AsignacionGuia>> guiasPorSalida = asignacionGuiaRepository.findBySalidaIdSalidaIn(idsSalida).stream()
                .collect(Collectors.groupingBy(a -> a.getId().getIdSalida()));
        Map<Long, List<String>> refugiosPorSalida = asignacionRefugioRepository
                .findBySalidaIdSalidaInOrderByOrdenVisitaAsc(idsSalida).stream()
                .collect(Collectors.groupingBy(a -> a.getId().getIdSalida(), LinkedHashMap::new,
                        Collectors.mapping(a -> a.getRefugio().getNombre(), Collectors.toList())));

        return asignaciones.stream().map(a -> {
            Salida s = a.getSalida();
            Excursion e = s.getExcursion();
            List<AsignacionGuia> guias = guiasPorSalida.getOrDefault(s.getIdSalida(), List.of());
            String responsable = guias.stream().filter(AsignacionGuia::esResponsable)
                    .map(g -> g.getGuia().getNombreCompleto()).findFirst().orElse("—");
            List<String> ayudantes = guias.stream().filter(g -> !g.esResponsable())
                    .map(g -> g.getGuia().getNombreCompleto()).sorted().toList();

            return new SalidaGuiaResponse(
                    s.getIdSalida(), e.getNombre(), s.getFechaInicio(), s.getFechaFinalizacion(),
                    s.getHoraInicio().format(HORA), s.getHoraFinalizacion().format(HORA),
                    s.getEstadoSalida(), a.getTipoGuia(), s.getCupoMaximo(), s.getCuposOcupados(),
                    responsable, ayudantes, refugiosPorSalida.getOrDefault(s.getIdSalida(), List.of()),
                    s.getObservaciones(), e.isRequiereCertificado(),
                    e.getEquipamientoObligatorio().stream().map(EquipamientoResponse::de).toList());
        }).toList();
    }
}
