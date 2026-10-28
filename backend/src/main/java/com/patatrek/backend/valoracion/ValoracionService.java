package com.patatrek.backend.valoracion;

import com.patatrek.backend.excepcion.DatosInvalidosException;
import com.patatrek.backend.excepcion.RecursoNoEncontradoException;
import com.patatrek.backend.excepcion.ReglaNegocioException;
import com.patatrek.backend.excursion.Excursion;
import com.patatrek.backend.excursion.ExcursionRepository;
import com.patatrek.backend.notificacion.NotificacionService;
import com.patatrek.backend.reserva.EstadoAsistencia;
import com.patatrek.backend.reserva.EstadoRequisitos;
import com.patatrek.backend.reserva.Participante;
import com.patatrek.backend.reserva.ParticipanteRepository;
import com.patatrek.backend.reserva.ReservaService;
import com.patatrek.backend.salida.EstadoSalida;
import com.patatrek.backend.salida.Salida;
import com.patatrek.backend.usuario.Cliente;
import com.patatrek.backend.valoracion.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Paquete "Valoraciones": CU-28 Registrar valoración de excursión y CU-29 Consultar valoraciones de excursiones.
 */
@Service
public class ValoracionService {

    static final String MENSAJE_SIN_CALIFICACION = "Debe seleccionar una calificación entre 1 y 5 estrellas para continuar";
    static final String MENSAJE_DATOS_INCORRECTOS = "El número de reserva o el DNI no son correctos.";
    private static final int MAXIMO_COMENTARIO = 1000;

    private final ParticipanteRepository participanteRepository;
    private final ValoracionRepository valoracionRepository;
    private final ExcursionRepository excursionRepository;
    private final ReservaService reservaService;
    private final NotificacionService notificacionService;

    public ValoracionService(ParticipanteRepository participanteRepository, ValoracionRepository valoracionRepository,
                             ExcursionRepository excursionRepository, ReservaService reservaService,
                             NotificacionService notificacionService) {
        this.participanteRepository = participanteRepository;
        this.valoracionRepository = valoracionRepository;
        this.excursionRepository = excursionRepository;
        this.reservaService = reservaService;
        this.notificacionService = notificacionService;
    }

    // =====================================================================
    //  CU-28 Registrar valoración
    // =====================================================================

    /** Valoraciones que el cliente todavía no hizo (salidas finalizadas en las que estuvo Presente y Apto). */
    @Transactional(readOnly = true)
    public List<ValoracionPendienteResponse> pendientesDelCliente() {
        Cliente cliente = reservaService.clienteActual();
        List<Participante> participaciones = participanteRepository
                .findByDniAndReservaSalidaEstadoSalida(cliente.getDni(), EstadoSalida.FINALIZADA).stream()
                .filter(this::acreditado)
                .toList();
        if (participaciones.isEmpty()) return List.of();
        Set<Long> yaValoradas = valoracionRepository.findByParticipanteIdParticipanteIn(
                        participaciones.stream().map(Participante::getIdParticipante).toList()).stream()
                .map(v -> v.getParticipante().getIdParticipante())
                .collect(Collectors.toSet());
        return participaciones.stream()
                .filter(p -> !yaValoradas.contains(p.getIdParticipante()))
                .map(p -> {
                    Salida s = p.getReserva().getSalida();
                    return new ValoracionPendienteResponse(p.getReserva().getIdReserva(), p.getDni(),
                            s.getExcursion().getNombre(), s.getFechaInicio(), s.getFechaFinalizacion());
                })
                .toList();
    }

    /** Paso 2-3: identifica al participante y arma el formulario (o muestra la valoración ya hecha). */
    @Transactional(readOnly = true)
    public FormularioValoracionResponse formulario(Long idReserva, String dni) {
        Participante p = buscarHabilitado(idReserva, dni);
        Optional<Valoracion> existente = valoracionRepository.findByParticipanteIdParticipante(p.getIdParticipante());
        return armarFormulario(p, existente.orElse(null));
    }

    @Transactional
    public FormularioValoracionResponse registrar(ValoracionRequest datos) {
        if (datos == null || datos.idReserva() == null || vacio(datos.dni())) {
            throw new DatosInvalidosException(MENSAJE_DATOS_INCORRECTOS, Map.of("dni", MENSAJE_DATOS_INCORRECTOS));
        }
        // 5a: calificación no seleccionada o fuera de rango
        if (datos.calificacion() == null || datos.calificacion() < 1 || datos.calificacion() > 5) {
            throw new DatosInvalidosException(MENSAJE_SIN_CALIFICACION, Map.of("calificacion", MENSAJE_SIN_CALIFICACION));
        }
        String comentario = vacio(datos.comentario()) ? null : datos.comentario().trim();
        if (comentario != null && comentario.length() > MAXIMO_COMENTARIO) {
            throw new DatosInvalidosException("El comentario es demasiado largo.",
                    Map.of("comentario", "El comentario puede tener hasta " + MAXIMO_COMENTARIO + " caracteres."));
        }

        Participante p = buscarHabilitado(datos.idReserva(), datos.dni());
        if (valoracionRepository.existsByParticipanteIdParticipante(p.getIdParticipante())) {
            throw new ReglaNegocioException("Ya registraste tu valoración para esta salida. ¡Gracias!");
        }

        Valoracion v = valoracionRepository.save(
                new Valoracion(p, datos.calificacion(), comentario, LocalDateTime.now()));

        String excursion = p.getReserva().getSalida().getExcursion().getNombre();
        notificacionService.enviar(p.getCorreoElectronico(),
                "PataTrek — ¡Gracias por valorar " + excursion + "!",
                "Hola " + p.getNombre() + ":\n\n"
                        + "Registramos tu valoración de " + v.getCalificacion() + " estrella(s) para la excursión "
                        + excursion + ".\n¡Gracias por ayudarnos a mejorar!\n\nEquipo PataTrek");
        return armarFormulario(p, v);
    }

    /** Valida los datos del participante y las reglas de negocio de CU-28. */
    private Participante buscarHabilitado(Long idReserva, String dni) {
        String dniLimpio = dni == null ? "" : dni.replaceAll("\\D", "");
        Participante p = participanteRepository.findByReservaIdReservaAndDni(idReserva, dniLimpio)
                .orElseThrow(() -> new RecursoNoEncontradoException(MENSAJE_DATOS_INCORRECTOS));
        Salida salida = p.getReserva().getSalida();
        if (salida.getEstadoSalida() != EstadoSalida.FINALIZADA) {
            throw new ReglaNegocioException("La salida todavía no finalizó: vas a poder valorarla cuando el guía la dé por terminada.");
        }
        if (!acreditado(p)) {
            throw new ReglaNegocioException(
                    "Solo pueden valorar los participantes acreditados como Presente y Apto en el control de asistencia.");
        }
        return p;
    }

    private boolean acreditado(Participante p) {
        return p.getEstadoAsistencia() == EstadoAsistencia.PRESENTE && p.getEstadoRequisitos() == EstadoRequisitos.APTO;
    }

    private FormularioValoracionResponse armarFormulario(Participante p, Valoracion v) {
        Salida s = p.getReserva().getSalida();
        return new FormularioValoracionResponse(p.getReserva().getIdReserva(), p.getDni(), p.getNombreCompleto(),
                s.getExcursion().getNombre(), s.getFechaInicio(), s.getFechaFinalizacion(), "Presente y Apto",
                v != null, v == null ? null : v.getCalificacion(), v == null ? null : v.getComentario(),
                v == null ? null : v.getFecha());
    }

    // =====================================================================
    //  CU-29 Consultar valoraciones (Administrador)
    // =====================================================================

    /** Todas las excursiones con su promedio y cantidad de valoraciones (las sin valoraciones, con promedio null). */
    @Transactional(readOnly = true)
    public List<ResumenValoracionesResponse> resumen() {
        Map<Long, List<Valoracion>> porExcursion = valoracionRepository.findAllByOrderByFechaDesc().stream()
                .collect(Collectors.groupingBy(v -> excursionDe(v).getIdExcursion()));
        return excursionRepository.findAllByOrderByNombreAsc().stream()
                .map(e -> {
                    List<Valoracion> lista = porExcursion.getOrDefault(e.getIdExcursion(), List.of());
                    return new ResumenValoracionesResponse(e.getIdExcursion(), e.getNombre(), e.estaActiva(),
                            promedio(lista), lista.size());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public DetalleValoracionesResponse detalle(Long idExcursion) {
        Excursion excursion = excursionRepository.findById(idExcursion)
                .orElseThrow(() -> new RecursoNoEncontradoException("La excursión no existe."));
        List<Valoracion> lista = valoracionRepository.findAllByOrderByFechaDesc().stream()
                .filter(v -> excursionDe(v).getIdExcursion().equals(idExcursion))
                .toList();
        List<Long> porEstrellas = new ArrayList<>(List.of(0L, 0L, 0L, 0L, 0L));
        lista.forEach(v -> porEstrellas.set(v.getCalificacion() - 1, porEstrellas.get(v.getCalificacion() - 1) + 1));
        return new DetalleValoracionesResponse(excursion.getIdExcursion(), excursion.getNombre(), promedio(lista),
                lista.size(), porEstrellas,
                lista.stream().map(v -> {
                    Participante p = v.getParticipante();
                    Salida s = p.getReserva().getSalida();
                    return new DetalleValoracionesResponse.Item(v.getIdValoracion(), v.getCalificacion(),
                            v.getComentario(), v.getFecha(), p.getNombreCompleto(), s.getIdSalida(), s.getFechaInicio());
                }).toList());
    }

    private static Excursion excursionDe(Valoracion v) {
        return v.getParticipante().getReserva().getSalida().getExcursion();
    }

    /** Promedio redondeado a un decimal; null si no hay valoraciones. */
    private static Double promedio(List<Valoracion> lista) {
        if (lista.isEmpty()) return null;
        double p = lista.stream().mapToInt(Valoracion::getCalificacion).average().orElse(0);
        return Math.round(p * 10) / 10.0;
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
