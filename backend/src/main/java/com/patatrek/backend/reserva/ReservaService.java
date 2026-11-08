package com.patatrek.backend.reserva;

import com.patatrek.backend.auth.Rol;
import com.patatrek.backend.auth.UsuarioActual;
import com.patatrek.backend.certificado.CertificadoMedicoService;
import com.patatrek.backend.contingencia.ContingenciaRepository;
import com.patatrek.backend.contingencia.TipoResolucion;
import com.patatrek.backend.equipamiento.Equipamiento;
import com.patatrek.backend.excepcion.AccesoDenegadoException;
import com.patatrek.backend.excepcion.CredencialesInvalidasException;
import com.patatrek.backend.excepcion.DatosInvalidosException;
import com.patatrek.backend.excepcion.RecursoNoEncontradoException;
import com.patatrek.backend.excepcion.ReglaNegocioException;
import com.patatrek.backend.excursion.Excursion;
import com.patatrek.backend.notificacion.Correo;
import com.patatrek.backend.notificacion.NotificacionService;
import static com.patatrek.backend.notificacion.FormatoCorreo.*;
import com.patatrek.backend.pago.*;
import com.patatrek.backend.reserva.dto.*;
import com.patatrek.backend.reserva.dto.ReservaRequest.EquipamientoSolicitado;
import com.patatrek.backend.reserva.dto.ReservaRequest.ParticipanteRequest;
import com.patatrek.backend.salida.AsignacionRefugio;
import com.patatrek.backend.salida.AsignacionRefugioRepository;
import com.patatrek.backend.salida.EstadoSalida;
import com.patatrek.backend.salida.Salida;
import com.patatrek.backend.usuario.Cliente;
import com.patatrek.backend.usuario.ClienteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Paquete "Reservas y pagos" (Documento de Arquitectura):
 * CU-18 Realizar reserva y CU-26 Realizar pago.
 *
 * CORRECCIÓN DE LA CÁTEDRA — recursos retenidos:
 * al confirmar el resumen, la reserva queda "Pendiente de pago" durante 10 minutos
 * (reserva.fecha_hora_vencimiento_retencion) y aparta los recursos en columnas propias:
 *   - salida.cupos_retenidos
 *   - asignacion_refugio.plazas_retenidas
 *   - equipamiento.stock_retenido + asignacion_equipamiento.estado_asignacion = RETENIDO
 * Si el pago se aprueba, los recursos pasan a ocupados (CONSOLIDADO); si se rechaza o vence el plazo, se liberan.
 */
@Service
public class ReservaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);

    /** Regla de negocio de CU-18 y CU-26: no se reserva ni se paga con 72 horas o menos de anticipación. */
    static final long HORAS_MINIMAS_ANTICIPACION = 72;
    static final int EDAD_MINIMA = 18;
    static final String MOTIVO_VENCIMIENTO =
            "Venció el plazo de retención sin que se completara el pago. Se liberaron los cupos, las plazas y el equipamiento.";
    static final String MOTIVO_RECHAZO =
            "La pasarela rechazó el pago. Se liberaron los cupos, las plazas y el equipamiento retenidos.";

    private static final Pattern PATRON_DNI = Pattern.compile("\\d{7,8}");
    private static final Pattern PATRON_CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PATRON_TELEFONO = Pattern.compile("^[0-9+()\\-\\s]{6,20}$");

    private final EntityManager em;
    private final ReservaRepository reservaRepository;
    private final ParticipanteRepository participanteRepository;
    private final AsignacionEquipamientoRepository asignacionEquipamientoRepository;
    private final AsignacionRefugioRepository asignacionRefugioRepository;
    private final PagoRepository pagoRepository;
    private final ClienteRepository clienteRepository;
    private final CertificadoMedicoService certificadoMedicoService;
    private final PasarelaPagosSimulada pasarela;
    private final NotificacionService notificacionService;
    private final int minutosRetencion;
    private final String correoClienteDemo;
    private final UsuarioActual usuarioActual;
    private final boolean exigirLogin;
    private final ContingenciaRepository contingenciaRepository;

    public ReservaService(EntityManager em,
                          ReservaRepository reservaRepository,
                          ParticipanteRepository participanteRepository,
                          AsignacionEquipamientoRepository asignacionEquipamientoRepository,
                          AsignacionRefugioRepository asignacionRefugioRepository,
                          PagoRepository pagoRepository,
                          ClienteRepository clienteRepository,
                          PasarelaPagosSimulada pasarela,
                          NotificacionService notificacionService,
                          CertificadoMedicoService certificadoMedicoService,
                          ContingenciaRepository contingenciaRepository,
                          @Value("${patatrek.minutos-retencion:10}") int minutosRetencion,
                          @Value("${patatrek.cliente-demo-correo:sofia.ibarra@mail.com}") String correoClienteDemo,
                          UsuarioActual usuarioActual,
                          @Value("${patatrek.seguridad.exigir-login:false}") boolean exigirLogin
    ) {
        this.em = em;
        this.reservaRepository = reservaRepository;
        this.participanteRepository = participanteRepository;
        this.asignacionEquipamientoRepository = asignacionEquipamientoRepository;
        this.asignacionRefugioRepository = asignacionRefugioRepository;
        this.pagoRepository = pagoRepository;
        this.clienteRepository = clienteRepository;
        this.pasarela = pasarela;
        this.notificacionService = notificacionService;
        this.certificadoMedicoService = certificadoMedicoService;
        this.contingenciaRepository = contingenciaRepository;
        this.minutosRetencion = minutosRetencion;
        this.correoClienteDemo = correoClienteDemo;
        this.usuarioActual = usuarioActual;
        this.exigirLogin = exigirLogin;
    }

    // =====================================================================
    //  Cliente actual
    // =====================================================================

    /**
     * El cliente que inició sesión. Si nadie inició sesión como cliente y todavía no se exige login
     * (patatrek.seguridad.exigir-login=false), se usa la clienta de demostración.
     */
    @Transactional(readOnly = true)
    public Cliente clienteActual() {
        var sesion = usuarioActual.conRol(Rol.CLIENTE);
        if (sesion.isPresent()) {
            return clienteRepository.findById(sesion.get().id())
                    .orElseThrow(() -> new CredencialesInvalidasException("Tu sesión ya no es válida. Iniciá sesión de nuevo."));
        }
        if (exigirLogin) {
            throw new CredencialesInvalidasException("Iniciá sesión como cliente para continuar.");
        }
        return clienteRepository.findByCorreoElectronicoIgnoreCase(correoClienteDemo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el cliente de demostración " + correoClienteDemo + "."));
    }

    /**
     * CU-21 Listar reservas realizadas: solo las reservas en las que el cliente es titular.
     * Filtros opcionales: rango de fechas de la salida y estado de la reserva (si se usan los dos, se cumplen ambos).
     */
    @Transactional
    public List<ReservaClienteResponse> misReservas(LocalDate desde, LocalDate hasta, EstadoReserva estado) {
        validarRango(desde, hasta);
        Cliente cliente = clienteActual();
        List<Reserva> reservas = reservaRepository.findByClienteIdUsuarioOrderByFechaHoraDesc(cliente.getIdUsuario());
        reservas.forEach(this::cancelarSiVencio);
        List<Reserva> filtradas = reservas.stream()
                .filter(r -> cumpleFiltros(r, desde, hasta, estado))
                .toList();
        Map<Long, String> estadosPago = estadosDePago(filtradas);
        return filtradas.stream()
                .map(r -> new ReservaClienteResponse(r.getIdReserva(), r.getSalida().getExcursion().getNombre(),
                        r.getSalida().getFechaInicio(), r.getSalida().getFechaFinalizacion(), r.getSalida().getHoraInicio(),
                        r.getEstadoReserva(), r.getCantidadParticipantes(), r.getMontoTotal(), r.getSaldoPendiente(),
                        estadosPago.get(r.getIdReserva()), r.getFechaHora(), r.isReprogramacionPendiente()))
                .toList();
    }

    // =====================================================================
    //  CU-20 Listar reservas generales (Administrador)
    // =====================================================================

    /**
     * Filtros opcionales (RF-17): nombre de excursión (contiene), ID de salida, rango de fechas de la salida,
     * DNI del cliente y estado de la reserva.
     */
    @Transactional
    public List<ReservaAdminResponse> listarReservasGenerales(String excursion, Long idSalida, LocalDate desde,
                                                              LocalDate hasta, String dni, EstadoReserva estado) {
        validarRango(desde, hasta);
        String textoExcursion = vacio(excursion) ? null : excursion.trim().toLowerCase(Locale.ROOT);
        String textoDni = vacio(dni) ? null : dni.replaceAll("\\D", "");

        List<Reserva> reservas = reservaRepository.findAllByOrderByFechaHoraDesc();
        reservas.forEach(this::cancelarSiVencio);
        List<Reserva> filtradas = reservas.stream()
                .filter(r -> cumpleFiltros(r, desde, hasta, estado))
                .filter(r -> textoExcursion == null
                        || r.getSalida().getExcursion().getNombre().toLowerCase(Locale.ROOT).contains(textoExcursion))
                .filter(r -> idSalida == null || idSalida.equals(r.getSalida().getIdSalida()))
                .filter(r -> textoDni == null || textoDni.isEmpty() || r.getCliente().getDni().contains(textoDni))
                .toList();
        Map<Long, String> estadosPago = estadosDePago(filtradas);
        return filtradas.stream()
                .map(r -> new ReservaAdminResponse(r.getIdReserva(), r.getFechaHora(),
                        r.getSalida().getExcursion().getNombre(), r.getSalida().getIdSalida(),
                        r.getSalida().getFechaInicio(), r.getSalida().getFechaFinalizacion(),
                        r.getCliente().getApellido() + ", " + r.getCliente().getNombre(), r.getCliente().getDni(),
                        r.getCliente().getCorreoElectronico(), r.getCantidadParticipantes(), r.getEstadoReserva(),
                        estadosPago.get(r.getIdReserva()), r.getMontoTotal(), r.getSaldoPendiente()))
                .toList();
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new DatosInvalidosException("Revisá el rango de fechas.",
                    Map.of("hasta", "La fecha \"hasta\" no puede ser anterior a la fecha \"desde\"."));
        }
    }

    private static boolean cumpleFiltros(Reserva r, LocalDate desde, LocalDate hasta, EstadoReserva estado) {
        LocalDate fecha = r.getSalida().getFechaInicio();
        return (estado == null || r.getEstadoReserva() == estado)
                && (desde == null || !fecha.isBefore(desde))
                && (hasta == null || !fecha.isAfter(hasta));
    }

    /**
     * Estado general del pago de cada reserva (RF-17): APROBADO si tiene algún pago aprobado,
     * RECHAZADO si el último intento fue rechazado, PENDIENTE si todavía no pagó.
     * (Las reservas de ejemplo no tienen registros de pago: si ya tienen algo abonado, se toman como APROBADO.)
     */
    private Map<Long, String> estadosDePago(List<Reserva> reservas) {
        Map<Long, String> estados = new HashMap<>();
        reservas.forEach(r -> estados.put(r.getIdReserva(),
                r.getMontoTotal().compareTo(r.getSaldoPendiente()) > 0 ? EstadoPago.APROBADO.name() : EstadoPago.PENDIENTE.name()));
        if (reservas.isEmpty()) return estados;
        List<Pago> pagos = new ArrayList<>(pagoRepository.findByReservaIdReservaIn(
                reservas.stream().map(Reserva::getIdReserva).toList()));
        pagos.sort(Comparator.comparing(Pago::getIdPago));
        Set<Long> conAprobado = new HashSet<>();
        for (Pago p : pagos) {
            Long id = p.getReserva().getIdReserva();
            if (p.getModoPago() == ModoPago.REINTEGRO && p.getEstadoPago() == EstadoPago.APROBADO) {
                // CU-27: lo abonado ya se reintegró
                estados.put(id, EstadoPago.DEVUELTO.name());
            } else if (p.getEstadoPago() == EstadoPago.APROBADO || p.getEstadoPago() == EstadoPago.DEVUELTO) {
                conAprobado.add(id);
                estados.put(id, EstadoPago.APROBADO.name());
            } else if (!conAprobado.contains(id)) {
                estados.put(id, p.getEstadoPago().name());
            }
        }
        return estados;
    }

    // =====================================================================
    //  CU-23 Cancelar reserva (Cliente)
    // =====================================================================

    /**
     * El cliente cancela una reserva Señada o Confirmada de una salida Disponible o Completa.
     * Se liberan los cupos, las plazas de refugio y el equipamiento prestado.
     * Regla de negocio: no hay devolución; lo abonado queda retenido en concepto de gastos administrativos.
     */
    @Transactional
    public ReservaResponse cancelar(Long idReserva, String motivo) {
        Reserva reserva = buscarReserva(idReserva);
        if (!reserva.getCliente().getIdUsuario().equals(clienteActual().getIdUsuario())) {
            throw new AccesoDenegadoException("Solo el cliente titular puede cancelar la reserva.");
        }
        Salida salida = buscarSalida(reserva.getSalida().getIdSalida(), true);
        String motivoNoCancelable = motivoNoCancelable(reserva);
        if (motivoNoCancelable != null) {
            throw new ReglaNegocioException(motivoNoCancelable);
        }

        List<String> serviciosLiberados = liberarRecursosConsolidados(reserva, salida);

        String motivoLimpio = vacio(motivo) ? null : motivo.trim();
        reserva.setEstadoReserva(EstadoReserva.CANCELADA);
        reserva.setMotivoCancelacion(motivoLimpio == null
                ? "Cancelada por el cliente (no indicó un motivo)."
                : "Cancelada por el cliente. Motivo: " + motivoLimpio);

        // CU-23 paso 5: correo al cliente con los detalles de la cancelación
        BigDecimal abonado = reserva.getMontoTotal().subtract(reserva.getSaldoPendiente()).max(BigDecimal.ZERO);
        Correo.Builder correo = Correo.para(reserva.getCliente().getCorreoElectronico())
                .asunto("PataTrek — Cancelaste la reserva #" + reserva.getIdReserva())
                .titulo("Tu reserva quedó cancelada")
                .saludo("Hola " + reserva.getCliente().getNombre() + ":")
                .parrafo("Confirmamos la cancelación de tu reserva. Estos son los detalles:")
                .dato("Reserva", "#" + reserva.getIdReserva() + " — " + salida.getExcursion().getNombre())
                .dato("Salida", salida(salida.getFechaInicio(), salida.getFechaFinalizacion(), salida.getHoraInicio()))
                .dato("Participantes", String.valueOf(reserva.getCantidadParticipantes()))
                .dato("Motivo", motivoLimpio == null ? "No indicado" : motivoLimpio)
                .dato("Servicios liberados", String.join(" · ", serviciosLiberados))
                .datoDestacado("Importe abonado", pesos(abonado));
        correo.nota(abonado.signum() > 0
                        ? "Según las condiciones de PataTrek, en una cancelación voluntaria el importe abonado no se devuelve: "
                                + "queda retenido en concepto de gastos administrativos."
                        : "No se realizó ningún cobro por esta reserva.")
                .boton("Ver otras excursiones", notificacionService.enlace("/"));
        notificacionService.enviar(correo.build());
        log.info("Reserva #{} cancelada por el cliente", reserva.getIdReserva());
        return respuesta(reserva);
    }

    /**
     * Libera los recursos ya consolidados de una reserva pagada (cupos, plazas de refugio y equipamiento prestado).
     * La usan la cancelación del cliente (CU-23) y la cancelación automática (CU-30).
     * Devuelve el detalle de lo liberado, para el correo.
     */
    public List<String> liberarRecursosConsolidados(Reserva reserva, Salida salida) {
        List<String> liberado = new ArrayList<>();
        int n = reserva.getCantidadParticipantes();
        salida.setCuposOcupados(Math.max(0, salida.getCuposOcupados() - n));
        if (salida.getEstadoSalida() == EstadoSalida.COMPLETA && salida.getCuposDisponibles() > 0) {
            salida.setEstadoSalida(EstadoSalida.DISPONIBLE);
        }
        liberado.add(n == 1 ? "1 lugar en la salida" : n + " lugares en la salida");
        if (reserva.getModalidadAlojamiento() == ModalidadAlojamiento.REFUGIO) {
            for (AsignacionRefugio a : refugiosDe(salida)) {
                a.setPlazasOcupadas(Math.max(0, a.getPlazasOcupadas() - n));
            }
            liberado.add(n == 1 ? "1 plaza de refugio" : n + " plazas de refugio");
        }
        // El equipamiento prestado vuelve a estar disponible (deja de contarse como CONSOLIDADO).
        List<String> equipo = new ArrayList<>();
        for (AsignacionEquipamiento a : asignacionEquipamientoRepository.findByReservaIdReserva(reserva.getIdReserva())) {
            if (a.getEstadoAsignacion() != EstadoAsignacion.LIBERADO) {
                a.setEstadoAsignacion(EstadoAsignacion.LIBERADO);
                equipo.add(a.getCantidadSolicitada() + " " + a.getEquipamiento().getNombre());
            }
        }
        if (!equipo.isEmpty()) {
            liberado.add("equipamiento: " + String.join(", ", equipo));
        }
        return liberado;
    }

    /** null si la reserva se puede cancelar; si no, el motivo. */
    private String motivoNoCancelable(Reserva r) {
        if (r.getEstadoReserva() != EstadoReserva.SENADA && r.getEstadoReserva() != EstadoReserva.CONFIRMADA) {
            return "Solo se puede cancelar una reserva Señada o Confirmada.";
        }
        EstadoSalida estadoSalida = r.getSalida().getEstadoSalida();
        if (estadoSalida != EstadoSalida.DISPONIBLE && estadoSalida != EstadoSalida.COMPLETA) {
            return "La salida ya está en curso, finalizada o cancelada: la reserva no se puede cancelar.";
        }
        return null;
    }

    // =====================================================================
    //  Salidas que el cliente puede reservar
    // =====================================================================

    @Transactional(readOnly = true)
    public List<SalidaReservableResponse> salidasDeExcursion(Long idExcursion) {
        Excursion excursion = em.find(Excursion.class, idExcursion);
        if (excursion == null || !excursion.estaActiva()) {
            throw new RecursoNoEncontradoException("La excursión no existe o no está disponible.");
        }
        List<Salida> salidas = em.createQuery("""
                        select s from Salida s
                        where s.excursion.idExcursion = :id
                          and s.estadoSalida in :estados
                          and s.fechaInicio >= :hoy
                        order by s.fechaInicio, s.horaInicio""", Salida.class)
                .setParameter("id", idExcursion)
                .setParameter("estados", EnumSet.of(EstadoSalida.DISPONIBLE, EstadoSalida.COMPLETA))
                .setParameter("hoy", LocalDate.now())
                .getResultList();

        Set<Long> conRefugio = salidas.isEmpty() ? Set.of()
                : asignacionRefugioRepository.findBySalidaIdSalidaInOrderByOrdenVisitaAsc(
                        salidas.stream().map(Salida::getIdSalida).toList()).stream()
                .map(a -> a.getSalida().getIdSalida()).collect(Collectors.toSet());

        return salidas.stream().map(s -> {
            String motivo = motivoNoReservable(s);
            return new SalidaReservableResponse(s.getIdSalida(), s.getFechaInicio(), s.getFechaFinalizacion(),
                    s.getHoraInicio(), s.getHoraFinalizacion(), Math.max(0, s.getCuposDisponibles()),
                    s.getEstadoSalida(), conRefugio.contains(s.getIdSalida()), motivo == null, motivo,
                    s.recorridoVigente());
        }).toList();
    }

    /** null si la salida se puede reservar; si no, el motivo para mostrar al cliente. */
    private String motivoNoReservable(Salida s) {
        if (s.getEstadoSalida() == EstadoSalida.COMPLETA || s.getCuposDisponibles() <= 0) {
            return "No quedan cupos disponibles.";
        }
        if (s.getEstadoSalida() != EstadoSalida.DISPONIBLE) {
            return "La salida no está disponible.";
        }
        if (!faltanMasDe72Horas(s)) {
            return "Las reservas cierran 72 horas antes del inicio de la salida.";
        }
        return null;
    }

    private boolean faltanMasDe72Horas(Salida s) {
        LocalDateTime inicio = s.getFechaInicio().atTime(s.getHoraInicio());
        return LocalDateTime.now().plusHours(HORAS_MINIMAS_ANTICIPACION).isBefore(inicio);
    }

    // =====================================================================
    //  CU-18 paso 1: datos para el asistente de reserva
    // =====================================================================

    @Transactional(readOnly = true)
    public DatosReservaResponse datosParaReservar(Long idSalida) {
        Salida s = buscarSalida(idSalida, false);
        String motivo = motivoNoReservable(s);
        if (motivo != null) {
            throw new ReglaNegocioException(motivo);
        }
        Excursion e = s.getExcursion();
        List<AsignacionRefugio> refugios = refugiosDe(s);
        int plazasRefugio = refugios.isEmpty() ? 0
                : refugios.stream().mapToInt(this::plazasDisponibles).min().orElse(0);

        List<DatosReservaResponse.EquipamientoDisponible> equipo = equipamientoObligatorio(e).stream()
                .map(item -> new DatosReservaResponse.EquipamientoDisponible(item.getIdEquipamiento(),
                        item.getNombre(), item.getDescripcion(), Math.max(0, disponibles(item, s))))
                .toList();

        return new DatosReservaResponse(s.getIdSalida(), e.getIdExcursion(), e.getNombre(), e.getImagenPromocional(),
                e.getDificultad().name(), e.getPrecio(), e.isRequiereCertificado(), e.isAceptaMenores(),
                s.getFechaInicio(), s.getFechaFinalizacion(), s.getHoraInicio(), s.getHoraFinalizacion(),
                esMultiDia(s), s.getCuposDisponibles(), minutosRetencion,
                refugios.stream().map(a -> new DatosReservaResponse.RefugioDisponible(a.getRefugio().getNombre(),
                        a.getOrdenVisita(), a.getRefugio().getPrecioNoche(), plazasDisponibles(a))).toList(),
                plazasRefugio, alojamientoPorPersona(refugios), equipo);
    }

    // =====================================================================
    //  CU-18: registrar la reserva y RETENER los recursos durante 10 minutos
    // =====================================================================

    @Transactional
    public ReservaResponse crear(ReservaRequest datos) {
        if (datos == null || datos.idSalida() == null) {
            throw new DatosInvalidosException("Elegí la salida que querés reservar.",
                    Map.of("idSalida", "Elegí una salida."));
        }
        // Bloqueo de la fila de la salida: dos clientes no pueden retener el mismo último cupo a la vez.
        Salida salida = buscarSalida(datos.idSalida(), true);
        Excursion excursion = salida.getExcursion();

        String motivo = motivoNoReservable(salida);
        if (motivo != null) {
            throw new ReglaNegocioException(motivo);
        }

        List<ParticipanteRequest> participantes = datos.participantes() == null ? List.of() : datos.participantes();
        if (participantes.isEmpty()) {
            throw new DatosInvalidosException("Agregá al menos un participante.",
                    Map.of("participantes", "Agregá al menos un participante."));
        }
        int cantidad = participantes.size();

        // 4a: más participantes que cupos disponibles
        if (cantidad > salida.getCuposDisponibles()) {
            throw ReglaNegocioException.enCampo("cantidadParticipantes",
                    "No hay cupos suficientes: quedan " + salida.getCuposDisponibles()
                            + " y querés reservar para " + cantidad + ". Modificá la cantidad de participantes.");
        }

        validarParticipantes(participantes, salida, excursion);

        // Alojamiento (solo salidas de más de un día)
        List<AsignacionRefugio> refugios = refugiosDe(salida);
        ModalidadAlojamiento modalidad = null;
        if (esMultiDia(salida)) {
            modalidad = datos.modalidadAlojamiento();
            if (modalidad == null) {
                throw new DatosInvalidosException("Elegí la modalidad de alojamiento.",
                        Map.of("modalidadAlojamiento", "Elegí si van a dormir en refugio o en carpa propia."));
            }
            if (modalidad == ModalidadAlojamiento.REFUGIO) {
                if (refugios.isEmpty()) {
                    throw ReglaNegocioException.enCampo("modalidadAlojamiento",
                            "Esta salida no tiene refugios asignados. Elegí \"Carpa propia\".");
                }
                // 10a: sin plazas de refugio suficientes
                for (AsignacionRefugio a : refugios) {
                    if (plazasDisponibles(a) < cantidad) {
                        throw ReglaNegocioException.enCampo("modalidadAlojamiento",
                                "No hay plazas suficientes en " + a.getRefugio().getNombre() + " (quedan "
                                        + plazasDisponibles(a) + "). Elegí \"Carpa propia\" o reducí la cantidad de participantes.");
                    }
                }
            }
        }

        // Equipamiento obligatorio solicitado en préstamo
        Map<Long, Integer> pedidos = validarEquipamiento(datos.equipamiento(), excursion, salida, cantidad);

        // ---------- Registro de la reserva "Pendiente de pago" ----------
        LocalDateTime ahora = LocalDateTime.now();
        BigDecimal monto = excursion.getPrecio().multiply(BigDecimal.valueOf(cantidad));
        if (modalidad == ModalidadAlojamiento.REFUGIO) {
            monto = monto.add(alojamientoPorPersona(refugios).multiply(BigDecimal.valueOf(cantidad)));
        }

        Reserva reserva = new Reserva();
        reserva.setSalida(salida);
        reserva.setCliente(clienteActual());
        reserva.setFechaHora(ahora);
        reserva.setEstadoReserva(EstadoReserva.PENDIENTE_PAGO);
        reserva.setCantidadParticipantes(cantidad);
        reserva.setMontoTotal(monto);
        reserva.setSaldoPendiente(monto);
        reserva.setModalidadAlojamiento(modalidad);
        reserva.setFechaHoraVencimientoRetencion(ahora.plusMinutes(minutosRetencion));
        reserva = reservaRepository.save(reserva);

        for (ParticipanteRequest p : participantes) {
            participanteRepository.save(new Participante(reserva, p.dni().trim(), p.nombre().trim(),
                    p.apellido().trim(), p.correoElectronico().trim(), p.nroTelefono().trim(), p.fechaNacimiento()));
        }

        // Crea los certificados médicos en estado PENDIENTE si la excursión los requiere
        certificadoMedicoService.crearPendientes(reserva);

        // ---------- Retención de recursos ----------
        salida.setCuposRetenidos(salida.getCuposRetenidos() + cantidad);
        if (modalidad == ModalidadAlojamiento.REFUGIO) {
            for (AsignacionRefugio a : refugios) {
                a.setPlazasRetenidas(a.getPlazasRetenidas() + cantidad);
            }
        }
        for (Map.Entry<Long, Integer> pedido : pedidos.entrySet()) {
            Equipamiento item = em.find(Equipamiento.class, pedido.getKey(), LockModeType.PESSIMISTIC_WRITE);
            item.setStockRetenido(item.getStockRetenido() + pedido.getValue());
            asignacionEquipamientoRepository.save(
                    new AsignacionEquipamiento(item, reserva, pedido.getValue(), EstadoAsignacion.RETENIDO));
        }

        log.info("Reserva #{} retenida hasta {} ({} participantes)", reserva.getIdReserva(),
                reserva.getFechaHoraVencimientoRetencion(), cantidad);
        return respuesta(reserva);
    }

    private void validarParticipantes(List<ParticipanteRequest> participantes, Salida salida, Excursion excursion) {
        Map<String, String> errores = new LinkedHashMap<>();
        Set<String> dnis = new HashSet<>();

        for (int i = 0; i < participantes.size(); i++) {
            ParticipanteRequest p = participantes.get(i);
            String pre = "participantes[" + i + "].";
            if (p == null) {
                errores.put(pre + "dni", "Completá los datos del participante.");
                continue;
            }
            if (vacio(p.dni())) {
                errores.put(pre + "dni", "El DNI es obligatorio.");
            } else if (!PATRON_DNI.matcher(p.dni().trim()).matches()) {
                errores.put(pre + "dni", "El DNI debe tener 7 u 8 números, sin puntos.");
            } else if (!dnis.add(p.dni().trim())) {
                errores.put(pre + "dni", "Este DNI ya está cargado en otro participante de la reserva.");
            }
            if (vacio(p.nombre())) errores.put(pre + "nombre", "El nombre es obligatorio.");
            if (vacio(p.apellido())) errores.put(pre + "apellido", "El apellido es obligatorio.");
            if (vacio(p.correoElectronico())) {
                errores.put(pre + "correoElectronico", "El correo electrónico es obligatorio.");
            } else if (!PATRON_CORREO.matcher(p.correoElectronico().trim()).matches()) {
                errores.put(pre + "correoElectronico", "Ingresá un correo válido, por ejemplo nombre@mail.com.");
            }
            if (vacio(p.nroTelefono())) {
                errores.put(pre + "nroTelefono", "El teléfono es obligatorio.");
            } else if (!PATRON_TELEFONO.matcher(p.nroTelefono().trim()).matches()) {
                errores.put(pre + "nroTelefono", "Ingresá solo números, espacios o guiones.");
            }
            if (p.fechaNacimiento() == null) {
                errores.put(pre + "fechaNacimiento", "La fecha de nacimiento es obligatoria.");
            } else if (!p.fechaNacimiento().isBefore(LocalDate.now())) {
                errores.put(pre + "fechaNacimiento", "La fecha de nacimiento debe ser anterior a hoy.");
            } else if (!excursion.isAceptaMenores()
                    && Period.between(p.fechaNacimiento(), salida.getFechaInicio()).getYears() < EDAD_MINIMA) {
                // 7a: la excursión no admite menores
                errores.put(pre + "fechaNacimiento",
                        "Esta excursión no admite menores de edad: el participante tendría menos de 18 años el día de la salida.");
            }
        }
        if (!errores.isEmpty()) {
            throw new DatosInvalidosException("Revisá los datos de los participantes marcados.", errores);
        }
    }

    /** Devuelve idEquipamiento → cantidad a prestar (solo cantidades mayores a 0). */
    private Map<Long, Integer> validarEquipamiento(List<EquipamientoSolicitado> solicitados, Excursion excursion,
                                                   Salida salida, int cantidadParticipantes) {
        Map<Long, Integer> pedidos = new LinkedHashMap<>();
        if (solicitados != null) {
            for (EquipamientoSolicitado e : solicitados) {
                if (e != null && e.idEquipamiento() != null) {
                    pedidos.put(e.idEquipamiento(), e.cantidad() == null ? 0 : e.cantidad());
                }
            }
        }
        Map<String, String> errores = new LinkedHashMap<>();
        Map<Long, Integer> aPrestar = new LinkedHashMap<>();
        Set<Long> obligatorios = new HashSet<>();

        for (Equipamiento item : equipamientoObligatorio(excursion)) {
            obligatorios.add(item.getIdEquipamiento());
            String campo = "equipamiento." + item.getIdEquipamiento();
            int cantidad = pedidos.getOrDefault(item.getIdEquipamiento(), 0);
            if (cantidad < 0) {
                errores.put(campo, "La cantidad no puede ser negativa.");
            } else if (cantidad > cantidadParticipantes) {
                errores.put(campo, "Podés pedir como máximo una unidad por participante (" + cantidadParticipantes + ").");
            } else if (cantidad > 0) {
                // 13a: stock insuficiente → informar la cantidad máxima disponible
                Equipamiento bloqueado = em.find(Equipamiento.class, item.getIdEquipamiento(), LockModeType.PESSIMISTIC_WRITE);
                int disponibles = Math.max(0, disponibles(bloqueado, salida));
                if (cantidad > disponibles) {
                    errores.put(campo, "Solo hay " + disponibles + " unidad(es) disponible(s) de " + item.getNombre()
                            + " para estas fechas. Ajustá la cantidad.");
                } else {
                    aPrestar.put(item.getIdEquipamiento(), cantidad);
                }
            }
        }
        for (Long id : pedidos.keySet()) {
            if (!obligatorios.contains(id) && pedidos.get(id) > 0) {
                errores.put("equipamiento." + id, "Ese ítem no es equipamiento obligatorio de esta excursión.");
            }
        }
        if (!errores.isEmpty()) {
            throw new ReglaNegocioException("Revisá el equipamiento solicitado.", errores);
        }
        return aPrestar;
    }

    // =====================================================================
    //  Consulta de la reserva
    // =====================================================================

    @Transactional
    public ReservaResponse obtener(Long idReserva) {
        Reserva reserva = buscarReserva(idReserva);
        cancelarSiVencio(reserva);
        return respuesta(reserva);
    }

    // =====================================================================
    //  CU-26 Realizar pago (invocado desde CU-18 o CU-19)
    // =====================================================================

    @Transactional
    public ResultadoPagoResponse pagar(Long idReserva, PagoRequest datos) {
        Reserva reserva = buscarReserva(idReserva);
        // Bloqueo de la salida: la consolidación modifica sus cupos.
        Salida salida = buscarSalida(reserva.getSalida().getIdSalida(), true);

        if (cancelarSiVencio(reserva)) {
            return new ResultadoPagoResponse(null, null, null,
                    "Venció el plazo de " + minutosRetencion + " minutos para pagar. La reserva se canceló y no se realizó ningún cobro.",
                    respuesta(reserva));
        }

        // 2a: plazo mínimo de anticipación
        if (!faltanMasDe72Horas(salida)) {
            throw new ReglaNegocioException("No se pueden procesar pagos con 72 horas o menos antes del inicio de la salida.");
        }
        ModoPago modo = datos.modoPago();
        if (!modosDisponibles(reserva).contains(modo)) {
            throw ReglaNegocioException.enCampo("modoPago", "Ese modo de pago no está disponible para esta reserva.");
        }

        BigDecimal importe = switch (modo) {
            case SENA -> sena(reserva.getMontoTotal());
            case TOTAL -> reserva.getMontoTotal();
            case SALDO -> reserva.getSaldoPendiente();
            case REINTEGRO -> throw ReglaNegocioException.enCampo("modoPago", "Ese modo de pago no está disponible para esta reserva.");
        };

        // Paso 5: el pago se registra "Pendiente" antes de invocar a la pasarela.
        Pago pago = pagoRepository.save(new Pago(reserva, importe, modo));

        // Pasos 6-7: la pasarela procesa el cobro y decide si lo aprueba (sin que PataTrek vea datos de tarjeta).
        PasarelaPagosSimulada.Respuesta r = pasarela.procesar(pago, datos.aprobar());
        pago.setNroTransaccion(r.nroTransaccion());
        pago.setFechaPago(LocalDateTime.now());

        if (!r.aprobado()) {
            // 7a: rechazo → Pago "Rechazado"; CU-18 15a: la reserva nueva se cancela y libera lo retenido.
            pago.setEstadoPago(EstadoPago.RECHAZADO);
            String mensaje;
            if (reserva.getEstadoReserva() == EstadoReserva.PENDIENTE_PAGO) {
                liberar(reserva, MOTIVO_RECHAZO);
                mensaje = r.motivoRechazo() + " La reserva se canceló y se liberaron los lugares retenidos.";
                // El rechazo se informa en pantalla (CU-18 15a, CU-26 7a): la documentación no pide correo.
            } else {
                mensaje = r.motivoRechazo() + " Tu reserva sigue señada; podés intentar pagar el saldo nuevamente.";
            }
            return new ResultadoPagoResponse(pago.getIdPago(), pago.getEstadoPago(), pago.getNroTransaccion(),
                    mensaje, respuesta(reserva));
        }

        // Paso 8: pago aprobado
        pago.setEstadoPago(EstadoPago.APROBADO);
        BigDecimal saldo = reserva.getSaldoPendiente().subtract(importe).max(BigDecimal.ZERO);
        reserva.setSaldoPendiente(saldo);

        boolean primerPago = reserva.getEstadoReserva() == EstadoReserva.PENDIENTE_PAGO;
        if (primerPago) {
            consolidar(reserva, salida);
        }
        reserva.setEstadoReserva(saldo.signum() == 0 ? EstadoReserva.CONFIRMADA : EstadoReserva.SENADA);

        enviarComprobante(reserva, pago);
        if (primerPago) {
            avisarCargaDeCertificados(reserva, salida);
        }
        String mensaje = reserva.getEstadoReserva() == EstadoReserva.CONFIRMADA
                ? "¡Pago aprobado! Tu reserva está confirmada. Te enviamos el comprobante por correo."
                : "¡Pago aprobado! Tu reserva quedó señada. Te enviamos el comprobante por correo.";
        return new ResultadoPagoResponse(pago.getIdPago(), pago.getEstadoPago(), pago.getNroTransaccion(),
                mensaje, respuesta(reserva));
    }

    /** Pasa los recursos de "retenidos" a "ocupados" (postcondición de CU-18). */
    private void consolidar(Reserva reserva, Salida salida) {
        int n = reserva.getCantidadParticipantes();
        salida.setCuposRetenidos(Math.max(0, salida.getCuposRetenidos() - n));
        salida.setCuposOcupados(salida.getCuposOcupados() + n);
        if (reserva.getModalidadAlojamiento() == ModalidadAlojamiento.REFUGIO) {
            for (AsignacionRefugio a : refugiosDe(salida)) {
                a.setPlazasRetenidas(Math.max(0, a.getPlazasRetenidas() - n));
                a.setPlazasOcupadas(a.getPlazasOcupadas() + n);
            }
        }
        for (AsignacionEquipamiento a : asignacionEquipamientoRepository.findByReservaIdReserva(reserva.getIdReserva())) {
            if (a.getEstadoAsignacion() == EstadoAsignacion.RETENIDO) {
                Equipamiento item = em.find(Equipamiento.class, a.getEquipamiento().getIdEquipamiento(),
                        LockModeType.PESSIMISTIC_WRITE);
                item.setStockRetenido(Math.max(0, item.getStockRetenido() - a.getCantidadSolicitada()));
                a.setEstadoAsignacion(EstadoAsignacion.CONSOLIDADO);
            }
        }
        reserva.setFechaHoraVencimientoRetencion(null);
        // Si se agotó el cupo, la salida pasa a "Completa".
        if (salida.getCuposDisponibles() <= 0 && salida.getEstadoSalida() == EstadoSalida.DISPONIBLE) {
            salida.setEstadoSalida(EstadoSalida.COMPLETA);
        }
    }

    /** CU-14 (cancelación de la salida): una reserva que todavía no pagó se cancela y libera lo retenido. */
    public void cancelarPendientePorContingencia(Reserva reserva, String motivo) {
        liberar(reserva, motivo);
    }

    /**
     * RF-12: el cliente responde a la reprogramación de la salida desde su panel.
     * Si acepta, la reserva sigue igual. Si rechaza, pasa a "Pendiente de devolución" (se le reintegra todo con CU-27)
     * y se liberan en ese momento los cupos, las plazas de refugio y el equipamiento.
     */
    @Transactional
    public ReservaResponse responderReprogramacion(Long idReserva, boolean acepta) {
        Reserva reserva = buscarReserva(idReserva);
        if (!reserva.getCliente().getIdUsuario().equals(clienteActual().getIdUsuario())) {
            throw new AccesoDenegadoException("Solo el cliente titular puede responder la reprogramación.");
        }
        if (!reserva.isReprogramacionPendiente() || !EstadoReserva.VIGENTES.contains(reserva.getEstadoReserva())) {
            throw new ReglaNegocioException("Esta reserva no tiene una reprogramación pendiente de respuesta.");
        }
        Salida salida = buscarSalida(reserva.getSalida().getIdSalida(), true);
        reserva.setReprogramacionPendiente(false);
        if (!acepta) {
            liberarRecursosConsolidados(reserva, salida);
            reserva.setEstadoReserva(EstadoReserva.PENDIENTE_DEVOLUCION);
            reserva.setMotivoCancelacion("Rechazo de la reprogramación de la salida por contingencia (nueva fecha: "
                    + salida(salida.getFechaInicio(), salida.getFechaFinalizacion(), salida.getHoraInicio()) + ").");
            log.info("Reserva #{}: el cliente rechazó la reprogramación", idReserva);
        } else {
            log.info("Reserva #{}: el cliente aceptó la reprogramación", idReserva);
        }
        return respuesta(reserva);
    }

    /** Cancela la reserva pendiente y devuelve lo retenido (CU-18 14a y 15a). */
    private void liberar(Reserva reserva, String motivo) {
        Salida salida = reserva.getSalida();
        int n = reserva.getCantidadParticipantes();
        salida.setCuposRetenidos(Math.max(0, salida.getCuposRetenidos() - n));
        if (reserva.getModalidadAlojamiento() == ModalidadAlojamiento.REFUGIO) {
            for (AsignacionRefugio a : refugiosDe(salida)) {
                a.setPlazasRetenidas(Math.max(0, a.getPlazasRetenidas() - n));
            }
        }
        for (AsignacionEquipamiento a : asignacionEquipamientoRepository.findByReservaIdReserva(reserva.getIdReserva())) {
            if (a.getEstadoAsignacion() == EstadoAsignacion.RETENIDO) {
                Equipamiento item = em.find(Equipamiento.class, a.getEquipamiento().getIdEquipamiento(),
                        LockModeType.PESSIMISTIC_WRITE);
                item.setStockRetenido(Math.max(0, item.getStockRetenido() - a.getCantidadSolicitada()));
                a.setEstadoAsignacion(EstadoAsignacion.LIBERADO);
            }
        }
        reserva.setEstadoReserva(EstadoReserva.CANCELADA);
        reserva.setMotivoCancelacion(motivo);
        reserva.setFechaHoraVencimientoRetencion(null);
    }

    // =====================================================================
    //  Vencimiento de la retención (CU-18 14a)
    // =====================================================================

    /** Cancela todas las reservas cuyo plazo de retención venció. La invoca la tarea programada. */
    @Transactional
    public int liberarVencidas() {
        List<Reserva> vencidas = reservaRepository.findByEstadoReservaAndFechaHoraVencimientoRetencionBefore(
                EstadoReserva.PENDIENTE_PAGO, LocalDateTime.now());
        for (Reserva r : vencidas) {
            buscarSalida(r.getSalida().getIdSalida(), true);
            vencer(r);
        }
        return vencidas.size();
    }

    /** SOLO PARA LA DEMOSTRACIÓN: adelanta el vencimiento para no esperar los 10 minutos. */
    @Transactional
    public ReservaResponse simularVencimiento(Long idReserva) {
        Reserva reserva = buscarReserva(idReserva);
        if (reserva.getEstadoReserva() != EstadoReserva.PENDIENTE_PAGO) {
            throw new ReglaNegocioException("Solo se puede simular el vencimiento de una reserva pendiente de pago.");
        }
        buscarSalida(reserva.getSalida().getIdSalida(), true);
        vencer(reserva);
        return respuesta(reserva);
    }

    /** true si la reserva estaba pendiente y su plazo ya venció (y por lo tanto se canceló ahora). */
    private boolean cancelarSiVencio(Reserva reserva) {
        if (reserva.getEstadoReserva() == EstadoReserva.PENDIENTE_PAGO
                && reserva.getFechaHoraVencimientoRetencion() != null
                && !LocalDateTime.now().isBefore(reserva.getFechaHoraVencimientoRetencion())) {
            vencer(reserva);
            return true;
        }
        return false;
    }

    private void vencer(Reserva reserva) {
        liberar(reserva, MOTIVO_VENCIMIENTO);
        log.info("Reserva #{} cancelada por vencimiento de la retención", reserva.getIdReserva());
        // CU-18 14a: se cancela y se liberan los recursos; la documentación no pide correo.
    }

    // =====================================================================
    //  Auxiliares
    // =====================================================================

    private Salida buscarSalida(Long idSalida, boolean bloquear) {
        Salida s = bloquear
                ? em.find(Salida.class, idSalida, LockModeType.PESSIMISTIC_WRITE)
                : em.find(Salida.class, idSalida);
        if (s == null) {
            throw new RecursoNoEncontradoException("La salida no existe.");
        }
        return s;
    }

    private Reserva buscarReserva(Long idReserva) {
        return reservaRepository.findById(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("La reserva #" + idReserva + " no existe."));
    }

    private List<AsignacionRefugio> refugiosDe(Salida s) {
        return asignacionRefugioRepository.findBySalidaIdSalidaInOrderByOrdenVisitaAsc(List.of(s.getIdSalida()));
    }

    private int plazasDisponibles(AsignacionRefugio a) {
        return Math.max(0, a.getPlazasReservadas() - a.getPlazasOcupadas() - a.getPlazasRetenidas());
    }

    /** Una noche en cada refugio del recorrido. */
    private BigDecimal alojamientoPorPersona(List<AsignacionRefugio> refugios) {
        return refugios.stream().map(a -> a.getRefugio().getPrecioNoche()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean esMultiDia(Salida s) {
        return s.getFechaFinalizacion() != null && s.getFechaFinalizacion().isAfter(s.getFechaInicio());
    }

    private List<Equipamiento> equipamientoObligatorio(Excursion e) {
        return e.getEquipamientoObligatorio().stream()
                .sorted(Comparator.comparing(Equipamiento::getNombre)).toList();
    }

    /** Stock − retenido por reservas en curso − prestado a reservas vigentes de salidas que se superponen. */
    private int disponibles(Equipamiento item, Salida s) {
        long prestadas = asignacionEquipamientoRepository.unidadesPrestadasEnPeriodo(
                item.getIdEquipamiento(), s.getFechaInicio(), s.getFechaFinalizacion());
        return (int) (item.getCantidadStock() - item.getStockRetenido() - prestadas);
    }

    private static BigDecimal sena(BigDecimal monto) {
        return monto.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    private List<ModoPago> modosDisponibles(Reserva r) {
        return switch (r.getEstadoReserva()) {
            case PENDIENTE_PAGO -> List.of(ModoPago.SENA, ModoPago.TOTAL);
            case SENADA -> r.getSaldoPendiente().signum() > 0 ? List.of(ModoPago.SALDO) : List.of();
            default -> List.of();
        };
    }

    private String motivoNoPuedePagar(Reserva r) {
        if (modosDisponibles(r).isEmpty()) {
            return switch (r.getEstadoReserva()) {
                case CONFIRMADA -> "La reserva ya está paga.";
                case CANCELADA -> "La reserva está cancelada.";
                default -> "La reserva no admite pagos.";
            };
        }
        if (!faltanMasDe72Horas(r.getSalida())) {
            return "No se pueden procesar pagos con 72 horas o menos antes del inicio de la salida.";
        }
        return null;
    }

    /**
     * Enlace de acceso del participante (ID de reserva + DNI, CU-19 y CU-24) para cargar su propio certificado.
     * Se manda una sola vez, con el primer pago aprobado: recién ahí la reserva queda Señada o Confirmada y admite
     * certificados (precondición del CU-24). Solo si la excursión lo exige, y no al titular (los carga desde su cuenta).
     */
    private void avisarCargaDeCertificados(Reserva r, Salida s) {
        if (!s.getExcursion().isRequiereCertificado()) {
            return;
        }
        String correoTitular = r.getCliente().getCorreoElectronico();
        LocalDateTime limite = s.getFechaInicio().atTime(s.getHoraInicio()).minusHours(HORAS_MINIMAS_ANTICIPACION);
        Set<String> avisados = new HashSet<>();
        for (Participante p : participanteRepository.findByReservaIdReservaOrderByIdParticipanteAsc(r.getIdReserva())) {
            String correo = p.getCorreoElectronico();
            if (vacio(correo) || correo.trim().equalsIgnoreCase(correoTitular)
                    || !avisados.add(correo.trim().toLowerCase() + "|" + p.getDni())) {
                continue;
            }
            notificacionService.enviar(Correo.para(correo.trim())
                    .asunto("PataTrek — Cargá tu certificado médico para " + s.getExcursion().getNombre())
                    .titulo("Cargá tu certificado médico")
                    .saludo("Hola " + p.getNombre() + ":")
                    .parrafo(r.getCliente().getNombre() + " " + r.getCliente().getApellido()
                            + " te anotó en una excursión de PataTrek que exige certificado médico. "
                            + "Podés cargar el tuyo desde el botón, sin necesidad de crear una cuenta."
                            + (r.getEstadoReserva() == EstadoReserva.SENADA
                            ? " Desde ahí también podés pagar el saldo pendiente de la reserva." : ""))
                    .dato("Excursión", s.getExcursion().getNombre())
                    .dato("Salida", salida(s.getFechaInicio(), s.getFechaFinalizacion(), s.getHoraInicio()))
                    .dato("Reserva", "#" + r.getIdReserva())
                    .dato("Tu DNI", p.getDni())
                    .datoDestacado("Plazo para cargarlo", "hasta el " + fechaHora(limite) + " h")
                    .dato("Formatos", "PDF, JPG o PNG, hasta 5 MB")
                    .boton("Ver la reserva y cargar mi certificado", notificacionService.enlace(
                            "/participante/reservas/" + r.getIdReserva() + "?dni=" + p.getDni()))
                    .nota("Si ningún participante de la reserva presenta su certificado antes del plazo, la reserva "
                            + "se cancela automáticamente sin devolución. Si no podés abrir el botón, entrá a PataTrek "
                            + "con el N.º de reserva y tu DNI.")
                    .build());
        }
    }

    /** CU-26 paso 8 / CU-18: comprobante de pago al cliente titular. */
    private void enviarComprobante(Reserva r, Pago pago) {
        Salida s = r.getSalida();
        boolean confirmada = r.getEstadoReserva() == EstadoReserva.CONFIRMADA;
        String modo = switch (pago.getModoPago()) {
            case SENA -> "Seña (50 %)";
            case TOTAL -> "Pago completo";
            case SALDO -> "Saldo restante";
            case REINTEGRO -> "Reintegro";
        };
        Correo.Builder correo = Correo.para(r.getCliente().getCorreoElectronico())
                .asunto("PataTrek — Comprobante de pago de la reserva #" + r.getIdReserva())
                .titulo("¡Recibimos tu pago!")
                .saludo("Hola " + r.getCliente().getNombre() + ":")
                .parrafo(confirmada
                        ? "Este es el comprobante de tu pago. Tu reserva está confirmada."
                        : "Este es el comprobante de tu pago. Tu reserva quedó señada.")
                .dato("Reserva", "#" + r.getIdReserva() + " — " + s.getExcursion().getNombre())
                .dato("Salida", salida(s.getFechaInicio(), s.getFechaFinalizacion(), s.getHoraInicio()))
                .dato("Participantes", String.valueOf(r.getCantidadParticipantes()))
                .dato("Modo de pago", modo)
                .dato("N.º de transacción", pago.getNroTransaccion())
                .dato("Fecha del pago", fechaHora(pago.getFechaPago()))
                .datoDestacado("Importe abonado", pesos(pago.getImporte()));
        if (!confirmada) {
            correo.dato("Saldo pendiente", pesos(r.getSaldoPendiente()))
                    .nota("Podés pagar el saldo desde Mis reservas hasta 72 horas antes de la salida. "
                            + "Si no se completa el pago en ese plazo, la reserva se cancela automáticamente sin devolución de la seña.");
        }
        correo.boton("Ver mi reserva", notificacionService.enlace("/reservas/" + r.getIdReserva()));
        notificacionService.enviar(correo.build());
    }

    private ReservaResponse respuesta(Reserva r) {
        Salida s = r.getSalida();
        Excursion e = s.getExcursion();
        long segundos = r.getEstadoReserva() == EstadoReserva.PENDIENTE_PAGO && r.getFechaHoraVencimientoRetencion() != null
                ? Math.max(0, Duration.between(LocalDateTime.now(), r.getFechaHoraVencimientoRetencion()).getSeconds())
                : 0;
        String motivoNoPago = motivoNoPuedePagar(r);

        List<ReservaResponse.RefugioResumen> refugios = r.getModalidadAlojamiento() == ModalidadAlojamiento.REFUGIO
                ? refugiosDe(s).stream()
                .map(a -> new ReservaResponse.RefugioResumen(a.getRefugio().getNombre(), a.getRefugio().getPrecioNoche()))
                .toList()
                : List.of();

        return new ReservaResponse(
                r.getIdReserva(), r.getEstadoReserva(), r.getFechaHora(), r.getCantidadParticipantes(),
                r.getMontoTotal(), sena(r.getMontoTotal()), r.getSaldoPendiente(), r.getModalidadAlojamiento(),
                r.getFechaHoraVencimientoRetencion(), segundos, r.getMotivoCancelacion(),
                motivoNoPago == null ? modosDisponibles(r) : List.of(), motivoNoPago,
                new ReservaResponse.SalidaResumen(s.getIdSalida(), e.getIdExcursion(), e.getNombre(), s.getFechaInicio(),
                        s.getFechaFinalizacion(), s.getHoraInicio(), s.getHoraFinalizacion(), e.getPrecio(),
                        e.isRequiereCertificado(), s.recorridoVigente()),
                participanteRepository.findByReservaIdReservaOrderByIdParticipanteAsc(r.getIdReserva()).stream()
                        .map(p -> new ReservaResponse.ParticipanteResumen(p.getDni(), p.getNombre(), p.getApellido(),
                                p.getCorreoElectronico()))
                        .toList(),
                refugios,
                asignacionEquipamientoRepository.findByReservaIdReserva(r.getIdReserva()).stream()
                        .sorted(Comparator.comparing(a -> a.getEquipamiento().getNombre()))
                        .map(a -> new ReservaResponse.EquipamientoResumen(a.getEquipamiento().getNombre(),
                                a.getCantidadSolicitada(), a.getEstadoAsignacion()))
                        .toList(),
                pagoRepository.findByReservaIdReservaOrderByIdPagoAsc(r.getIdReserva()).stream()
                        .map(p -> new ReservaResponse.PagoResumen(p.getIdPago(), p.getFechaPago(), p.getImporte(),
                                p.getModoPago(), p.getEstadoPago(), p.getNroTransaccion()))
                        .toList(),
                r.getMontoTotal().subtract(r.getSaldoPendiente()).max(BigDecimal.ZERO),
                motivoNoCancelable(r) == null,
                reprogramacion(r));
    }

    private ReservaResponse.Reprogramacion reprogramacion(Reserva r) {
        if (!r.isReprogramacionPendiente() || !EstadoReserva.VIGENTES.contains(r.getEstadoReserva())) {
            return null;
        }
        return contingenciaRepository.findFirstBySalidaIdSalidaAndTipoResolucionOrderByFechaRegistroDesc(
                        r.getSalida().getIdSalida(), TipoResolucion.REPROGRAMACION.name())
                .map(c -> new ReservaResponse.Reprogramacion(c.getFechaAnteriorSalida(), c.getMotivo(),
                        c.getRecorridoAlternativo()))
                .orElse(new ReservaResponse.Reprogramacion(null, null, null));
    }
}