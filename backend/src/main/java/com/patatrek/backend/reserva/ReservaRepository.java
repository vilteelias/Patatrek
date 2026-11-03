package com.patatrek.backend.reserva;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Reservas "Pendiente de pago" cuyo plazo de retención de 10 minutos ya venció (RF-15). */
    List<Reserva> findByEstadoReservaAndFechaHoraVencimientoRetencionBefore(EstadoReserva estado, LocalDateTime momento);

    /** "Mis reservas" del cliente, de la más nueva a la más vieja. */
    @EntityGraph(attributePaths = {"salida", "salida.excursion"})
    List<Reserva> findByClienteIdUsuarioOrderByFechaHoraDesc(Long idCliente);

    /** Catálogo general del Administrador (CU-20), de la más nueva a la más vieja. */
    @EntityGraph(attributePaths = {"salida", "salida.excursion", "cliente"})
    List<Reserva> findAllByOrderByFechaHoraDesc();

    /** CU-27: reservas "Pendiente de devolución" y "Devuelta". */
    @EntityGraph(attributePaths = {"salida", "salida.excursion", "cliente"})
    List<Reserva> findByEstadoReservaInOrderByIdReservaAsc(java.util.Collection<EstadoReserva> estados);

    long countByEstadoReservaIn(java.util.Collection<EstadoReserva> estados);

    /** CU-30: reservas de una salida en los estados indicados, con su cliente titular. */
    @EntityGraph(attributePaths = {"cliente"})
    List<Reserva> findBySalidaIdSalidaAndEstadoReservaInOrderByIdReservaAsc(Long idSalida,
                                                                           java.util.Collection<EstadoReserva> estados);

    /** Reservas vigentes asociadas a una salida específica para aplicar contingencias. */
    @Query("SELECT r FROM Reserva r WHERE r.salida.idSalida = :idSalida AND r.estadoReserva IN ('CONFIRMADA', 'SENADA', 'PENDIENTE_PAGO')")
    List<Reserva> findVigentesBySalida(@Param("idSalida") Long idSalida);
}
