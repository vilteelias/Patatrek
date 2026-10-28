package com.patatrek.backend.equipamiento;

import com.patatrek.backend.equipamiento.dto.SalidaEquipamientoResponse;
import com.patatrek.backend.equipamiento.dto.StockEquipamientoResponse;
import com.patatrek.backend.excepcion.RecursoNoEncontradoException;
import com.patatrek.backend.reserva.AsignacionEquipamiento;
import com.patatrek.backend.reserva.AsignacionEquipamientoRepository;
import com.patatrek.backend.reserva.EstadoAsignacion;
import com.patatrek.backend.salida.EstadoSalida;
import com.patatrek.backend.salida.Salida;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Consulta (solo lectura) del stock del catálogo de equipamiento precargado.
 * El ERS deja fuera del alcance el alta, la baja y la reposición del stock: acá solo se muestra
 * cuánto hay, cuánto está retenido por reservas que esperan el pago (corrección de la cátedra)
 * y cuánto está prestado a reservas pagadas, con la misma cuenta que usa la reserva (CU-18 13a).
 */
@Service
public class StockEquipamientoService {

    private final EntityManager em;
    private final EquipamientoRepository equipamientoRepository;
    private final AsignacionEquipamientoRepository asignacionRepository;

    public StockEquipamientoService(EntityManager em, EquipamientoRepository equipamientoRepository,
                                    AsignacionEquipamientoRepository asignacionRepository) {
        this.em = em;
        this.equipamientoRepository = equipamientoRepository;
        this.asignacionRepository = asignacionRepository;
    }

    /** Salidas que todavía no terminaron (para elegir en el filtro). */
    @Transactional(readOnly = true)
    public List<SalidaEquipamientoResponse> salidas() {
        return em.createQuery("""
                        select s from Salida s join fetch s.excursion
                        where s.estadoSalida in :estados and s.fechaFinalizacion >= :hoy
                        order by s.fechaInicio, s.horaInicio""", Salida.class)
                .setParameter("estados", EnumSet.of(EstadoSalida.DISPONIBLE, EstadoSalida.COMPLETA, EstadoSalida.EN_CURSO))
                .setParameter("hoy", LocalDate.now())
                .getResultList().stream()
                .map(s -> new SalidaEquipamientoResponse(s.getIdSalida(), s.getExcursion().getNombre(),
                        s.getFechaInicio(), s.getFechaFinalizacion(), s.getEstadoSalida()))
                .toList();
    }

    @Transactional(readOnly = true)
    public StockEquipamientoResponse stock(Long idSalida) {
        Salida salida = null;
        LocalDate desde = LocalDate.now();
        LocalDate hasta = desde;
        if (idSalida != null) {
            salida = em.find(Salida.class, idSalida);
            if (salida == null) {
                throw new RecursoNoEncontradoException("No existe la salida #" + idSalida + ".");
            }
            desde = salida.getFechaInicio();
            hasta = salida.getFechaFinalizacion();
        }
        final LocalDate d = desde, h = hasta;

        List<StockEquipamientoResponse.Item> items = equipamientoRepository.findAllByOrderByNombreAsc().stream()
                .map(e -> {
                    int prestado = (int) asignacionRepository.unidadesPrestadasEnPeriodo(e.getIdEquipamiento(), d, h);
                    int disponible = Math.max(0, e.getCantidadStock() - e.getStockRetenido() - prestado);
                    return new StockEquipamientoResponse.Item(e.getIdEquipamiento(), e.getNombre(), e.getDescripcion(),
                            e.getCantidadStock(), e.getStockRetenido(), prestado, disponible);
                })
                .toList();

        List<StockEquipamientoResponse.Asignacion> asignaciones = List.of();
        StockEquipamientoResponse.SalidaResumen resumen = null;
        if (salida != null) {
            resumen = new StockEquipamientoResponse.SalidaResumen(salida.getIdSalida(),
                    salida.getExcursion().getNombre(), salida.getFechaInicio(), salida.getFechaFinalizacion(),
                    salida.getEstadoSalida());
            asignaciones = em.createQuery("""
                            select a from AsignacionEquipamiento a
                            join fetch a.equipamiento join fetch a.reserva r join fetch r.cliente
                            where r.salida.idSalida = :id and a.estadoAsignacion <> :liberado""",
                            AsignacionEquipamiento.class)
                    .setParameter("id", idSalida)
                    .setParameter("liberado", EstadoAsignacion.LIBERADO)
                    .getResultList().stream()
                    .sorted(Comparator.comparing((AsignacionEquipamiento a) -> a.getReserva().getIdReserva())
                            .thenComparing(a -> a.getEquipamiento().getNombre()))
                    .map(a -> new StockEquipamientoResponse.Asignacion(a.getReserva().getIdReserva(),
                            a.getReserva().getCliente().getApellido() + ", " + a.getReserva().getCliente().getNombre(),
                            a.getReserva().getEstadoReserva(), a.getEquipamiento().getNombre(),
                            a.getCantidadSolicitada(), a.getEstadoAsignacion()))
                    .toList();
        }
        return new StockEquipamientoResponse(resumen, desde, hasta, items, asignaciones);
    }
}
