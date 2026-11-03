package com.patatrek.backend.salida;

import com.patatrek.backend.contingencia.Contingencia;
import com.patatrek.backend.excepcion.DatosInvalidosException;
import com.patatrek.backend.excepcion.RecursoNoEncontradoException;
import com.patatrek.backend.excepcion.ReglaNegocioException;
import com.patatrek.backend.excursion.EstadoExcursion;
import com.patatrek.backend.excursion.Excursion;
import com.patatrek.backend.reserva.EstadoReserva;
import com.patatrek.backend.reserva.Reserva;
import com.patatrek.backend.salida.dto.*;
import com.patatrek.backend.salida.dto.SalidaAdminResponse.ContingenciaResumen;
import com.patatrek.backend.salida.dto.SalidaAdminResponse.GuiaResumen;
import com.patatrek.backend.salida.dto.SalidaAdminResponse.RefugioAsignado;
import com.patatrek.backend.usuario.Guia;
import com.patatrek.backend.usuario.GuiaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Paquete "Gestión de salidas" (Documento de Arquitectura), rol Administrador:
 * CU-09 Planificar salida, CU-10 Listar salidas, CU-11 Modificar salida, CU-12 Asignar personal y cupos
 * y CU-13 Dar de baja salida. CU-14 (contingencias) está en el paquete contingencia y usa las validaciones de acá.
 *
 * Cupos (RF-10): cupo base = capacidad estándar del guía responsable; variación = cupo base × 0,25 × N ayudantes
 * (redondeado hacia abajo); cupo total = cupo base + variación, con ocupados ≤ cupo total ≤ cupo máximo.
 */
@Service
public class GestionSalidaService {

    private static final Logger log = LoggerFactory.getLogger(GestionSalidaService.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Salidas que ocupan guías y refugios. */
    public static final Set<EstadoSalida> ACTIVAS = EnumSet.of(EstadoSalida.DISPONIBLE, EstadoSalida.COMPLETA,
            EstadoSalida.EN_CURSO);
    /** Reservas que ocupan un lugar en la salida (RF-10: Confirmadas + Señadas + Pendientes de pago). */
    public static final Set<EstadoReserva> OCUPAN_CUPO = EnumSet.of(EstadoReserva.SENADA, EstadoReserva.CONFIRMADA,
            EstadoReserva.PENDIENTE_PAGO);

    private final EntityManager em;
    private final GuiaRepository guiaRepository;
    private final RefugioRepository refugioRepository;
    private final AsignacionGuiaRepository asignacionGuiaRepository;
    private final AsignacionRefugioRepository asignacionRefugioRepository;

    public GestionSalidaService(EntityManager em, GuiaRepository guiaRepository, RefugioRepository refugioRepository,
                                AsignacionGuiaRepository asignacionGuiaRepository,
                                AsignacionRefugioRepository asignacionRefugioRepository) {
        this.em = em;
        this.guiaRepository = guiaRepository;
        this.refugioRepository = refugioRepository;
        this.asignacionGuiaRepository = asignacionGuiaRepository;
        this.asignacionRefugioRepository = asignacionRefugioRepository;
    }

    // =====================================================================
    //  CU-10 Listar salidas (vista del Administrador) y detalle
    // =====================================================================

    @Transactional(readOnly = true)
    public List<SalidaAdminResponse> listar(String excursion, EstadoSalida estado) {
        String texto = excursion == null ? "" : excursion.trim().toLowerCase(Locale.ROOT);
        List<Salida> salidas = em.createQuery(
                        "select s from Salida s join fetch s.excursion order by s.fechaInicio desc, s.horaInicio desc",
                        Salida.class)
                .getResultList().stream()
                .filter(s -> estado == null || s.getEstadoSalida() == estado)
                .filter(s -> texto.isEmpty() || s.getExcursion().getNombre().toLowerCase(Locale.ROOT).contains(texto))
                .toList();
        return armar(salidas, false);
    }

    @Transactional(readOnly = true)
    public SalidaAdminResponse detalle(Long idSalida) {
        return armar(List.of(buscar(idSalida, false)), true).get(0);
    }

    // =====================================================================
    //  Opciones para las pantallas (guías y refugios con disponibilidad)
    // =====================================================================

    @Transactional(readOnly = true)
    public OpcionesSalidaResponse opciones(LocalDate fechaInicio, LocalTime horaInicio, LocalDate fechaFinalizacion,
                                           LocalTime horaFinalizacion, Long excluirSalida) {
        List<OpcionesSalidaResponse.ExcursionOpcion> excursiones = em.createQuery(
                        "select e from Excursion e where e.estadoExcursion = :activa order by e.nombre", Excursion.class)
                .setParameter("activa", EstadoExcursion.ACTIVA)
                .getResultList().stream()
                .map(e -> new OpcionesSalidaResponse.ExcursionOpcion(e.getIdExcursion(), e.getNombre(),
                        e.getDuracion() == null ? 1 : e.getDuracion(), e.getRecorridoBase()))
                .toList();

        boolean conPeriodo = fechaInicio != null && horaInicio != null && fechaFinalizacion != null
                && horaFinalizacion != null && !fechaFinalizacion.isBefore(fechaInicio);
        LocalDateTime desde = conPeriodo ? fechaInicio.atTime(horaInicio) : null;
        LocalDateTime hasta = conPeriodo ? fechaFinalizacion.atTime(horaFinalizacion) : null;

        List<OpcionesSalidaResponse.GuiaOpcion> guias = guiaRepository.findAll().stream()
                .sorted(Comparator.comparing(Guia::getApellido).thenComparing(Guia::getNombre))
                .map(g -> {
                    String motivo = conPeriodo ? motivoGuiaNoDisponible(g, desde, hasta, excluirSalida) : null;
                    return new OpcionesSalidaResponse.GuiaOpcion(g.getIdUsuario(), nombre(g), g.getCapacidadEstandar(),
                            motivo == null, motivo);
                })
                .toList();

        List<OpcionesSalidaResponse.RefugioOpcion> refugios = refugioRepository.findAll().stream()
                .sorted(Comparator.comparing(Refugio::getNombre))
                .map(r -> new OpcionesSalidaResponse.RefugioOpcion(r.getIdRefugio(), r.getNombre(), r.getCapacidadPlazas(),
                        conPeriodo ? plazasLibres(r, fechaInicio, fechaFinalizacion, excluirSalida) : r.getCapacidadPlazas(),
                        r.getPrecioNoche()))
                .toList();
        return new OpcionesSalidaResponse(excursiones, guias, refugios);
    }

    // =====================================================================
    //  CU-09 Planificar salida (RF-08)
    // =====================================================================

    @Transactional
    public SalidaAdminResponse planificar(PlanificarSalidaRequest d) {
        Map<String, String> errores = new LinkedHashMap<>();

        Excursion excursion = null;
        if (d.idExcursion() == null) {
            errores.put("idExcursion", "Elegí la excursión base.");
        } else {
            excursion = em.find(Excursion.class, d.idExcursion());
            if (excursion == null || excursion.getEstadoExcursion() != EstadoExcursion.ACTIVA) {
                errores.put("idExcursion", "La excursión no existe o está dada de baja.");
                excursion = null;
            }
        }
        validarCronograma(d.fechaInicio(), d.horaInicio(), d.fechaFinalizacion(), d.horaFinalizacion(), excursion,
                errores);

        Guia responsable = null;
        if (d.idGuiaResponsable() == null) {
            errores.put("idGuiaResponsable", "Elegí el guía responsable.");
        } else {
            responsable = guiaRepository.findById(d.idGuiaResponsable()).orElse(null);
            if (responsable == null) {
                errores.put("idGuiaResponsable", "El guía elegido no existe.");
            }
        }

        if (d.cupoMaximo() == null || d.cupoMaximo() <= 0) {
            errores.put("cupoMaximo", "Ingresá el cupo máximo de la salida.");
        } else if (responsable != null && d.cupoMaximo() <= responsable.getCapacidadEstandar()) {
            errores.put("cupoMaximo", "El cupo máximo tiene que ser mayor que el cupo base del guía responsable ("
                    + responsable.getCapacidadEstandar() + ").");
        }

        boolean cronogramaValido = !errores.containsKey("fechaInicio") && !errores.containsKey("fechaFinalizacion")
                && !errores.containsKey("horaInicio") && !errores.containsKey("horaFinalizacion")
                && d.fechaInicio() != null && d.fechaFinalizacion() != null;

        // Regla: el guía responsable no puede tener otra salida que se superponga
        if (responsable != null && cronogramaValido) {
            String motivo = motivoGuiaNoDisponible(responsable, d.fechaInicio().atTime(d.horaInicio()),
                    d.fechaFinalizacion().atTime(d.horaFinalizacion()), null);
            if (motivo != null) {
                errores.put("idGuiaResponsable", motivo);
            }
        }

        // Regla: una salida de más de un día exige un refugio por cada noche
        List<Refugio> noches = List.of();
        if (cronogramaValido) {
            noches = validarRefugios(d.fechaInicio(), d.fechaFinalizacion(), d.plazasPorRefugio(), d.refugiosPorNoche(),
                    null, Map.of(), errores);
        }

        if (!errores.isEmpty()) {
            throw new DatosInvalidosException("Revisá los datos marcados.", errores);
        }

        Salida s = new Salida();
        s.setExcursion(excursion);
        s.setFechaInicio(d.fechaInicio());
        s.setFechaFinalizacion(d.fechaFinalizacion());
        s.setHoraInicio(d.horaInicio());
        s.setHoraFinalizacion(d.horaFinalizacion());
        s.setCupoBase(responsable.getCapacidadEstandar());
        s.setCupoMaximo(d.cupoMaximo());
        s.setCupoTotal(responsable.getCapacidadEstandar()); // sin ayudantes, el cupo total es el cupo base
        s.setCuposOcupados(0);
        s.setCuposRetenidos(0);
        s.setEstadoSalida(EstadoSalida.DISPONIBLE);
        s.setRecorrido(excursion.getRecorridoBase());
        em.persist(s);
        em.flush(); // necesitamos el id para las claves compuestas

        asignacionGuiaRepository.save(new AsignacionGuia(responsable, s, TipoGuia.RESPONSABLE));
        guardarRefugios(s, noches, d.plazasPorRefugio(), List.of());

        log.info("Salida #{} planificada: {} del {} ({} noches)", s.getIdSalida(), excursion.getNombre(),
                s.getFechaInicio().format(FECHA), noches.size());
        return detalle(s.getIdSalida());
    }

    // =====================================================================
    //  CU-11 Modificar salida (RF-10): guía responsable y refugios
    // =====================================================================

    @Transactional
    public SalidaAdminResponse modificar(Long idSalida, ModificarSalidaRequest d) {
        Salida s = buscar(idSalida, true);
        exigirModificable(s);
        Map<String, String> errores = new LinkedHashMap<>();

        List<AsignacionGuia> asignaciones = asignacionGuiaRepository.findBySalidaIdSalidaIn(List.of(idSalida));
        AsignacionGuia actual = asignaciones.stream().filter(AsignacionGuia::esResponsable).findFirst().orElse(null);
        int ayudantes = (int) asignaciones.stream().filter(a -> !a.esResponsable()).count();

        Guia nuevoResponsable = null;
        if (d.idGuiaResponsable() == null) {
            errores.put("idGuiaResponsable", "Elegí el guía responsable.");
        } else if (actual == null || !actual.getGuia().getIdUsuario().equals(d.idGuiaResponsable())) {
            nuevoResponsable = guiaRepository.findById(d.idGuiaResponsable()).orElse(null);
            if (nuevoResponsable == null) {
                errores.put("idGuiaResponsable", "El guía elegido no existe.");
            } else if (asignaciones.stream().anyMatch(a -> !a.esResponsable()
                    && a.getGuia().getIdUsuario().equals(d.idGuiaResponsable()))) {
                errores.put("idGuiaResponsable", nombre(nuevoResponsable)
                        + " es guía ayudante de esta salida. Quitalo de los ayudantes antes de hacerlo responsable.");
            } else {
                String motivo = motivoGuiaNoDisponible(nuevoResponsable, inicio(s), fin(s), idSalida);
                if (motivo != null) {
                    errores.put("idGuiaResponsable", motivo);
                } else if (s.getCupoMaximo() <= nuevoResponsable.getCapacidadEstandar()) {
                    errores.put("idGuiaResponsable", "La capacidad estándar de " + nombre(nuevoResponsable) + " ("
                            + nuevoResponsable.getCapacidadEstandar()
                            + ") no es menor al cupo máximo de la salida (" + s.getCupoMaximo() + ").");
                }
            }
        }

        // Refugios: las plazas ya ocupadas o retenidas no se pueden quitar
        List<AsignacionRefugio> refugiosActuales = refugiosDe(s);
        Map<Long, Integer> enUso = new HashMap<>();
        refugiosActuales.forEach(a -> enUso.put(a.getRefugio().getIdRefugio(), a.getPlazasOcupadas() + a.getPlazasRetenidas()));
        List<Refugio> noches = validarRefugios(s.getFechaInicio(), s.getFechaFinalizacion(), d.plazasPorRefugio(),
                d.refugiosPorNoche(), idSalida, enUso, errores);

        if (!errores.isEmpty()) {
            throw new DatosInvalidosException("Revisá los datos marcados.", errores);
        }

        if (nuevoResponsable != null) {
            // Cambiar el responsable cambia el cupo base y obliga a recalcular el cupo total (RF-10)
            int base = nuevoResponsable.getCapacidadEstandar();
            int total = cupoNuevo(base, ayudantes);
            exigirLimites(s, total, "No es posible cambiar el guía responsable");
            if (actual != null) {
                asignacionGuiaRepository.delete(actual);
                asignacionGuiaRepository.flush();
            }
            asignacionGuiaRepository.save(new AsignacionGuia(nuevoResponsable, s, TipoGuia.RESPONSABLE));
            s.setCupoBase(base);
            s.setCupoTotal(total);
            actualizarEstadoPorCupo(s);
        }
        guardarRefugios(s, noches, d.plazasPorRefugio(), refugiosActuales);
        return detalle(idSalida);
    }

    // =====================================================================
    //  CU-12 Asignar personal y cupos a salida
    // =====================================================================

    @Transactional
    public SalidaAdminResponse asignarAyudantes(Long idSalida, AyudantesRequest d) {
        Salida s = buscar(idSalida, true);
        exigirModificable(s);
        List<Long> ids = d.idsGuias() == null ? List.of() : d.idsGuias().stream().filter(Objects::nonNull).toList();
        if (new HashSet<>(ids).size() != ids.size()) {
            throw ReglaNegocioException.enCampo("idsGuias", "Un mismo guía no puede asignarse más de una vez a la salida.");
        }

        List<AsignacionGuia> asignaciones = asignacionGuiaRepository.findBySalidaIdSalidaIn(List.of(idSalida));
        AsignacionGuia responsable = asignaciones.stream().filter(AsignacionGuia::esResponsable).findFirst().orElse(null);
        if (responsable != null && ids.contains(responsable.getGuia().getIdUsuario())) {
            throw ReglaNegocioException.enCampo("idsGuias",
                    "El guía responsable de la salida no puede ser también guía ayudante.");
        }
        Map<Long, AsignacionGuia> actuales = asignaciones.stream().filter(a -> !a.esResponsable())
                .collect(Collectors.toMap(a -> a.getGuia().getIdUsuario(), a -> a));

        List<Guia> nuevos = new ArrayList<>();
        for (Long id : ids) {
            if (actuales.containsKey(id)) continue;
            Guia g = guiaRepository.findById(id)
                    .orElseThrow(() -> ReglaNegocioException.enCampo("idsGuias", "Uno de los guías elegidos no existe."));
            String motivo = motivoGuiaNoDisponible(g, inicio(s), fin(s), idSalida);
            if (motivo != null) {
                throw ReglaNegocioException.enCampo("idsGuias", motivo);
            }
            nuevos.add(g);
        }

        // Pasos 4 y 4a/4b: recálculo y límites
        int total = cupoNuevo(s.getCupoBase(), ids.size());
        int anterior = actuales.size();
        if (total < ocupados(s)) {
            throw new ReglaNegocioException("No es posible remover personal: las plazas ocupadas (" + ocupados(s)
                    + ") superan el cupo resultante (" + total + ").");
        }
        if (total > s.getCupoMaximo()) {
            throw new ReglaNegocioException("No es posible agregar personal: el cupo resultante (" + total
                    + ") superaría el cupo máximo permitido (" + s.getCupoMaximo() + ").");
        }

        actuales.forEach((id, a) -> {
            if (!ids.contains(id)) asignacionGuiaRepository.delete(a);
        });
        asignacionGuiaRepository.flush();
        nuevos.forEach(g -> asignacionGuiaRepository.save(new AsignacionGuia(g, s, TipoGuia.AYUDANTE)));
        s.setCupoTotal(total);
        actualizarEstadoPorCupo(s);
        log.info("Salida #{}: {} → {} guías ayudantes, cupo total {}", idSalida, anterior, ids.size(), total);
        return detalle(idSalida);
    }

    // =====================================================================
    //  CU-13 Dar de baja salida (RF-11)
    // =====================================================================

    @Transactional
    public SalidaAdminResponse darDeBaja(Long idSalida, String motivo) {
        Salida s = buscar(idSalida, true);
        exigirModificable(s);
        long activas = reservasQueOcupan(idSalida).size();
        if (activas > 0) {
            throw new ReglaNegocioException("No se puede dar de baja: la salida tiene " + activas
                    + (activas == 1 ? " reserva activa" : " reservas activas")
                    + ". Si hay que suspenderla con reservas, registrá una contingencia de cancelación.");
        }
        s.setEstadoSalida(EstadoSalida.CANCELADA);
        s.setMotivoCancelacion("Baja del " + LocalDateTime.now().format(FECHA_HORA) + ". Motivo: " + motivo.trim());
        liberarGuiasYRefugios(s);
        log.info("Salida #{} dada de baja", idSalida);
        return detalle(idSalida);
    }

    // =====================================================================
    //  Operaciones que usa CU-14 (contingencias)
    // =====================================================================

    /** Desvincula a los guías y libera las plazas de refugio de la salida (baja y contingencia de cancelación). */
    public void liberarGuiasYRefugios(Salida s) {
        asignacionGuiaRepository.deleteAll(asignacionGuiaRepository.findBySalidaIdSalidaIn(List.of(s.getIdSalida())));
        asignacionRefugioRepository.deleteAll(refugiosDe(s));
    }

    /** null si el guía está libre en ese período; si no, el motivo para mostrar. */
    public String motivoGuiaNoDisponible(Guia guia, LocalDateTime desde, LocalDateTime hasta, Long excluirSalida) {
        List<AsignacionGuia> otras = em.createQuery("""
                        select a from AsignacionGuia a join fetch a.salida s join fetch s.excursion
                        where a.guia.idUsuario = :guia and s.estadoSalida in :estados""", AsignacionGuia.class)
                .setParameter("guia", guia.getIdUsuario())
                .setParameter("estados", ACTIVAS)
                .getResultList();
        for (AsignacionGuia a : otras) {
            Salida o = a.getSalida();
            if (o.getIdSalida().equals(excluirSalida)) continue;
            if (inicio(o).isBefore(hasta) && desde.isBefore(fin(o))) {
                return nombre(guia) + " ya está asignado a la salida #" + o.getIdSalida() + " ("
                        + o.getExcursion().getNombre() + ", " + o.getFechaInicio().format(FECHA)
                        + ") que se superpone con esas fechas.";
            }
        }
        return null;
    }

    /** Plazas del refugio que no están reservadas por otras salidas activas que se superponen en esas fechas. */
    public int plazasLibres(Refugio refugio, LocalDate desde, LocalDate hasta, Long excluirSalida) {
        List<AsignacionRefugio> otras = em.createQuery("""
                        select a from AsignacionRefugio a join fetch a.salida s
                        where a.refugio.idRefugio = :refugio and s.estadoSalida in :estados""", AsignacionRefugio.class)
                .setParameter("refugio", refugio.getIdRefugio())
                .setParameter("estados", ACTIVAS)
                .getResultList();
        int usadas = otras.stream()
                .filter(a -> !a.getSalida().getIdSalida().equals(excluirSalida))
                .filter(a -> !a.getSalida().getFechaInicio().isAfter(hasta) && !desde.isAfter(a.getSalida().getFechaFinalizacion()))
                .mapToInt(AsignacionRefugio::getPlazasReservadas).sum();
        return Math.max(0, refugio.getCapacidadPlazas() - usadas);
    }

    public List<AsignacionRefugio> refugiosDe(Salida s) {
        return asignacionRefugioRepository.findBySalidaIdSalidaInOrderByOrdenVisitaAsc(List.of(s.getIdSalida()));
    }

    public List<Reserva> reservasQueOcupan(Long idSalida) {
        return em.createQuery("select r from Reserva r join fetch r.cliente where r.salida.idSalida = :id "
                        + "and r.estadoReserva in :estados order by r.idReserva", Reserva.class)
                .setParameter("id", idSalida)
                .setParameter("estados", OCUPAN_CUPO)
                .getResultList();
    }

    public Salida buscar(Long idSalida, boolean bloquear) {
        Salida s = bloquear ? em.find(Salida.class, idSalida, LockModeType.PESSIMISTIC_WRITE) : em.find(Salida.class, idSalida);
        if (s == null) {
            throw new RecursoNoEncontradoException("No existe la salida #" + idSalida + ".");
        }
        return s;
    }

    public static LocalDateTime inicio(Salida s) {
        return s.getFechaInicio().atTime(s.getHoraInicio());
    }

    public static LocalDateTime fin(Salida s) {
        return s.getFechaFinalizacion().atTime(s.getHoraFinalizacion());
    }

    public static String nombre(Guia g) {
        return g.getApellido() + ", " + g.getNombre();
    }

    // =====================================================================
    //  Reparación de datos viejos
    // =====================================================================

    /**
     * Las salidas planificadas con la versión anterior quedaron con cupo total 0 (no se reservaban).
     * Se recalcula con la fórmula de RF-10 a partir del cupo base y de los ayudantes asignados.
     */
    @Transactional
    public int repararCuposTotales() {
        List<Salida> sinCupo = em.createQuery(
                        "select s from Salida s where s.cupoTotal <= 0 and s.cupoBase > 0 and s.estadoSalida in :estados",
                        Salida.class)
                .setParameter("estados", EnumSet.of(EstadoSalida.DISPONIBLE, EstadoSalida.COMPLETA))
                .getResultList();
        for (Salida s : sinCupo) {
            long n = asignacionGuiaRepository.findBySalidaIdSalidaIn(List.of(s.getIdSalida())).stream()
                    .filter(a -> !a.esResponsable()).count();
            int total = Math.min(cupoNuevo(s.getCupoBase(), (int) n), Math.max(s.getCupoMaximo(), s.getCupoBase()));
            s.setCupoTotal(Math.max(total, ocupados(s)));
            actualizarEstadoPorCupo(s);
            log.info("Salida #{}: se corrigió el cupo total a {}", s.getIdSalida(), s.getCupoTotal());
        }
        return sinCupo.size();
    }

    // =====================================================================
    //  Auxiliares
    // =====================================================================

    /** Variación = cupo base × 0,25 × N (redondeada hacia abajo); cupo nuevo = cupo base + variación. */
    public static int cupoNuevo(int cupoBase, int ayudantes) {
        return cupoBase + (int) Math.floor(cupoBase * 0.25 * ayudantes);
    }

    public static int ocupados(Salida s) {
        return s.getCuposOcupados() + s.getCuposRetenidos();
    }

    private void exigirLimites(Salida s, int total, String accion) {
        if (total < ocupados(s)) {
            throw new ReglaNegocioException(accion + ": las plazas ocupadas (" + ocupados(s)
                    + ") superan el cupo resultante (" + total + ").");
        }
        if (total > s.getCupoMaximo()) {
            throw new ReglaNegocioException(accion + ": el cupo resultante (" + total
                    + ") superaría el cupo máximo permitido (" + s.getCupoMaximo() + ").");
        }
    }

    /** Si el cupo ocupado alcanza el cupo total la salida queda Completa; si no, Disponible. */
    public static void actualizarEstadoPorCupo(Salida s) {
        if (s.getEstadoSalida() != EstadoSalida.DISPONIBLE && s.getEstadoSalida() != EstadoSalida.COMPLETA) return;
        s.setEstadoSalida(ocupados(s) >= s.getCupoTotal() ? EstadoSalida.COMPLETA : EstadoSalida.DISPONIBLE);
    }

    private void exigirModificable(Salida s) {
        if (s.getEstadoSalida() != EstadoSalida.DISPONIBLE && s.getEstadoSalida() != EstadoSalida.COMPLETA) {
            throw new ReglaNegocioException("Solo se pueden modificar salidas en estado Disponible o Completa.");
        }
        if (!inicio(s).isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioException("La salida ya inició: no se puede modificar.");
        }
    }

    private void validarCronograma(LocalDate fi, LocalTime hi, LocalDate ff, LocalTime hf, Excursion excursion,
                                   Map<String, String> errores) {
        if (fi == null) errores.put("fechaInicio", "Ingresá la fecha de inicio.");
        if (hi == null) errores.put("horaInicio", "Ingresá la hora de inicio.");
        if (ff == null) errores.put("fechaFinalizacion", "Ingresá la fecha de finalización.");
        if (hf == null) errores.put("horaFinalizacion", "Ingresá la hora de finalización.");
        if (fi == null || hi == null || ff == null || hf == null) return;

        if (!fi.atTime(hi).isAfter(LocalDateTime.now())) {
            errores.put("fechaInicio", "La salida tiene que empezar en una fecha y hora futuras.");
        }
        if (ff.isBefore(fi)) {
            errores.put("fechaFinalizacion", "La fecha de finalización no puede ser anterior a la de inicio.");
            return;
        }
        if (ff.equals(fi) && !hf.isAfter(hi)) {
            errores.put("horaFinalizacion", "La hora de finalización tiene que ser posterior a la de inicio.");
        }
        if (excursion != null && excursion.getDuracion() != null) {
            long dias = ChronoUnit.DAYS.between(fi, ff) + 1;
            if (dias != excursion.getDuracion()) {
                LocalDate esperada = fi.plusDays(excursion.getDuracion() - 1);
                errores.put("fechaFinalizacion", "La excursión dura " + excursion.getDuracion()
                        + (excursion.getDuracion() == 1 ? " día" : " días") + ": la salida tiene que terminar el "
                        + esperada.format(FECHA) + ".");
            }
        }
    }

    /**
     * Regla de negocio de CU-09: una salida de más de un día exige un refugio por noche (un mismo refugio puede
     * repetirse). Devuelve el refugio de cada noche, en orden. enUso = plazas ocupadas/retenidas por refugio (CU-11).
     */
    private List<Refugio> validarRefugios(LocalDate fi, LocalDate ff, Integer plazas, List<Long> idsPorNoche,
                                          Long excluirSalida, Map<Long, Integer> enUso, Map<String, String> errores) {
        int cantidadNoches = (int) ChronoUnit.DAYS.between(fi, ff);
        if (cantidadNoches <= 0) {
            return List.of();
        }
        List<Long> ids = idsPorNoche == null ? List.of() : idsPorNoche;
        if (ids.size() != cantidadNoches || ids.stream().anyMatch(Objects::isNull)) {
            errores.put("refugiosPorNoche", "Elegí un refugio para cada una de las " + cantidadNoches
                    + (cantidadNoches == 1 ? " noche." : " noches."));
            return List.of();
        }
        if (plazas == null || plazas <= 0) {
            errores.put("plazasPorRefugio", "Ingresá cuántas plazas reservar en cada refugio.");
            return List.of();
        }
        List<Refugio> noches = new ArrayList<>();
        Set<Long> revisados = new HashSet<>();
        for (Long id : ids) {
            Refugio r = refugioRepository.findById(id).orElse(null);
            if (r == null) {
                errores.put("refugiosPorNoche", "Uno de los refugios elegidos no existe.");
                return List.of();
            }
            noches.add(r);
            if (!revisados.add(id)) continue;
            int libres = plazasLibres(r, fi, ff, excluirSalida);
            if (plazas > libres) {
                errores.put("plazasPorRefugio", "En " + r.getNombre() + " quedan " + libres
                        + " plazas libres para esas fechas.");
            }
            int usadas = enUso.getOrDefault(id, 0);
            if (plazas < usadas) {
                errores.put("plazasPorRefugio", "En " + r.getNombre() + " ya hay " + usadas
                        + " plazas ocupadas por reservas: no se pueden reservar menos.");
            }
        }
        enUso.forEach((id, usadas) -> {
            if (usadas > 0 && !ids.contains(id)) {
                String nombre = refugioRepository.findById(id).map(Refugio::getNombre).orElse("el refugio");
                errores.put("refugiosPorNoche", "No se puede quitar " + nombre + ": tiene " + usadas
                        + " plazas ocupadas por reservas.");
            }
        });
        return noches;
    }

    /** Guarda un registro por refugio (la clave es refugio + salida), con el orden de la primera noche en que aparece. */
    private void guardarRefugios(Salida s, List<Refugio> noches, Integer plazas, List<AsignacionRefugio> actuales) {
        Map<Long, Integer> orden = new LinkedHashMap<>();
        for (int i = 0; i < noches.size(); i++) {
            orden.putIfAbsent(noches.get(i).getIdRefugio(), i + 1);
        }
        Map<Long, AsignacionRefugio> existentes = actuales.stream()
                .collect(Collectors.toMap(a -> a.getRefugio().getIdRefugio(), a -> a));
        existentes.forEach((id, a) -> {
            if (!orden.containsKey(id)) asignacionRefugioRepository.delete(a);
        });
        asignacionRefugioRepository.flush();
        for (Map.Entry<Long, Integer> e : orden.entrySet()) {
            AsignacionRefugio a = existentes.get(e.getKey());
            if (a != null) {
                a.setOrdenVisita(e.getValue());
                a.setPlazasReservadas(plazas);
            } else {
                Refugio r = noches.stream().filter(x -> x.getIdRefugio().equals(e.getKey())).findFirst().orElseThrow();
                asignacionRefugioRepository.save(new AsignacionRefugio(r, s, e.getValue(), plazas));
            }
        }
    }

    private List<SalidaAdminResponse> armar(List<Salida> salidas, boolean conContingencias) {
        if (salidas.isEmpty()) return List.of();
        List<Long> ids = salidas.stream().map(Salida::getIdSalida).toList();
        Map<Long, List<AsignacionGuia>> guias = asignacionGuiaRepository.findBySalidaIdSalidaIn(ids).stream()
                .collect(Collectors.groupingBy(a -> a.getSalida().getIdSalida()));
        Map<Long, List<AsignacionRefugio>> refugios = asignacionRefugioRepository.findBySalidaIdSalidaInOrderByOrdenVisitaAsc(ids)
                .stream().collect(Collectors.groupingBy(a -> a.getSalida().getIdSalida()));
        Map<Long, Long> reservas = new HashMap<>();
        em.createQuery("select r.salida.idSalida, count(r) from Reserva r where r.salida.idSalida in :ids "
                        + "and r.estadoReserva in :estados group by r.salida.idSalida", Object[].class)
                .setParameter("ids", ids)
                .setParameter("estados", OCUPAN_CUPO)
                .getResultList()
                .forEach(f -> reservas.put((Long) f[0], (Long) f[1]));
        Map<Long, List<Contingencia>> contingencias = new HashMap<>();
        if (conContingencias) {
            em.createQuery("select c from Contingencia c where c.salida.idSalida in :ids order by c.fechaRegistro desc",
                            Contingencia.class)
                    .setParameter("ids", ids)
                    .getResultList()
                    .forEach(c -> contingencias.computeIfAbsent(c.getSalida().getIdSalida(), k -> new ArrayList<>()).add(c));
        }

        LocalDateTime ahora = LocalDateTime.now();
        List<SalidaAdminResponse> filas = new ArrayList<>();
        for (Salida s : salidas) {
            List<AsignacionGuia> ag = guias.getOrDefault(s.getIdSalida(), List.of());
            GuiaResumen responsable = ag.stream().filter(AsignacionGuia::esResponsable).findFirst()
                    .map(a -> resumen(a.getGuia())).orElse(null);
            List<GuiaResumen> ayudantes = ag.stream().filter(a -> !a.esResponsable()).map(a -> resumen(a.getGuia()))
                    .sorted(Comparator.comparing(GuiaResumen::nombre)).toList();
            List<RefugioAsignado> refs = refugios.getOrDefault(s.getIdSalida(), List.of()).stream()
                    .map(a -> new RefugioAsignado(a.getRefugio().getIdRefugio(), a.getRefugio().getNombre(),
                            a.getOrdenVisita(), a.getPlazasReservadas(), a.getPlazasOcupadas(), a.getPlazasRetenidas()))
                    .toList();
            int activas = reservas.getOrDefault(s.getIdSalida(), 0L).intValue();
            boolean iniciada = !inicio(s).isAfter(ahora);
            boolean modificable = !iniciada
                    && (s.getEstadoSalida() == EstadoSalida.DISPONIBLE || s.getEstadoSalida() == EstadoSalida.COMPLETA);
            Excursion e = s.getExcursion();
            filas.add(new SalidaAdminResponse(
                    s.getIdSalida(), e.getIdExcursion(), e.getNombre(), e.getDuracion() == null ? 1 : e.getDuracion(),
                    e.isRequiereCertificado(),
                    s.getFechaInicio(), s.getFechaFinalizacion(), s.getHoraInicio(), s.getHoraFinalizacion(),
                    s.getEstadoSalida(), s.getCupoBase(), s.getCupoMaximo(), s.getCupoTotal(), ocupados(s),
                    Math.max(0, s.getCuposDisponibles()), responsable, ayudantes, refs, s.getRecorrido(),
                    s.getMotivoCancelacion(), s.getObservaciones(), activas, iniciada, modificable,
                    modificable && activas == 0, ACTIVAS.contains(s.getEstadoSalida()),
                    contingencias.getOrDefault(s.getIdSalida(), List.of()).stream()
                            .map(c -> new ContingenciaResumen(c.getIdContingencia(), c.getFechaRegistro(),
                                    c.getTipoContingencia(), c.getTipoResolucion(), c.getMotivo(),
                                    c.getRecorridoAlternativo(), c.getFechaAnteriorSalida()))
                            .toList()));
        }
        return filas;
    }

    private static GuiaResumen resumen(Guia g) {
        return new GuiaResumen(g.getIdUsuario(), nombre(g), g.getCapacidadEstandar());
    }
}
