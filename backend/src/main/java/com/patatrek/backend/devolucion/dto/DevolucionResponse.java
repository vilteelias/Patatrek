package com.patatrek.backend.devolucion.dto;

import com.patatrek.backend.reserva.EstadoReserva;
import com.patatrek.backend.salida.EstadoSalida;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fila del listado de devoluciones (CU-27 paso 2): ID de reserva, cliente titular, salida asociada,
 * motivo de la cancelación o contingencia y monto total abonado.
 * En las reservas ya devueltas también trae el número y la fecha del reintegro.
 */
public record DevolucionResponse(
        Long idReserva,
        EstadoReserva estadoReserva,
        String cliente,
        String dniCliente,
        String correoCliente,
        String excursion,
        Long idSalida,
        LocalDate fechaInicio,
        LocalDate fechaFinalizacion,
        EstadoSalida estadoSalida,
        int cantidadParticipantes,
        String motivo,
        BigDecimal importeAbonado,
        LocalDateTime fechaPago,
        String nroTransaccionOriginal,
        String nroTransaccionReintegro,
        LocalDateTime fechaReintegro
) {}
