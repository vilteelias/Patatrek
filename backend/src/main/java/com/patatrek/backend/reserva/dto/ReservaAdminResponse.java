package com.patatrek.backend.reserva.dto;

import com.patatrek.backend.reserva.EstadoReserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fila del catálogo general de reservas del Administrador (CU-20 / RF-17):
 * ID de reserva, excursión y salida, cliente titular, cantidad de participantes,
 * estado de la reserva, estado general del pago y fecha de reserva.
 */
public record ReservaAdminResponse(
        Long idReserva,
        LocalDateTime fechaHora,
        String excursion,
        Long idSalida,
        LocalDate fechaInicio,
        LocalDate fechaFinalizacion,
        String cliente,
        String dniCliente,
        String correoCliente,
        int cantidadParticipantes,
        EstadoReserva estadoReserva,
        String estadoPago,
        BigDecimal montoTotal,
        BigDecimal saldoPendiente
) {}
