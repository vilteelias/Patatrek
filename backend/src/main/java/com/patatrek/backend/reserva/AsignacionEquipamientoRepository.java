package com.patatrek.backend.reserva;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AsignacionEquipamientoRepository extends JpaRepository<AsignacionEquipamiento, AsignacionEquipamientoId> {

    List<AsignacionEquipamiento> findByReservaIdReservaInAndEstadoAsignacionNot(
            Collection<Long> idsReserva, EstadoAsignacion estadoExcluido);

    @EntityGraph(attributePaths = "equipamiento")
    List<AsignacionEquipamiento> findByReservaIdReserva(Long idReserva);

    /**
     * Unidades de un ítem ya prestadas (consolidadas) a reservas vigentes de salidas que se superponen
     * en fechas con el período indicado: esas unidades no están disponibles para otra reserva.
     */
    @Query("""
            select coalesce(sum(a.cantidadSolicitada), 0) from AsignacionEquipamiento a
            where a.equipamiento.idEquipamiento = :idEquipamiento
              and a.estadoAsignacion = com.patatrek.backend.reserva.EstadoAsignacion.CONSOLIDADO
              and a.reserva.estadoReserva in (com.patatrek.backend.reserva.EstadoReserva.SENADA,
                                              com.patatrek.backend.reserva.EstadoReserva.CONFIRMADA)
              and a.reserva.salida.fechaInicio <= :hasta
              and a.reserva.salida.fechaFinalizacion >= :desde
            """)
    long unidadesPrestadasEnPeriodo(@Param("idEquipamiento") Long idEquipamiento,
                                    @Param("desde") LocalDate desde,
                                    @Param("hasta") LocalDate hasta);
}
