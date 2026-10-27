package com.patatrek.backend.reserva;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Reservas "Pendiente de pago" cuyo plazo de retención de 10 minutos ya venció (RF-15). */
    List<Reserva> findByEstadoReservaAndFechaHoraVencimientoRetencionBefore(EstadoReserva estado, LocalDateTime momento);

    /** "Mis reservas" del cliente, de la más nueva a la más vieja. */
    @EntityGraph(attributePaths = {"salida", "salida.excursion"})
    List<Reserva> findByClienteIdUsuarioOrderByFechaHoraDesc(Long idCliente);
}
