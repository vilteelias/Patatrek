package com.patatrek.backend.reserva.dto;

import com.patatrek.backend.pago.EstadoPago;

/** Respuesta de CU-26 al asistente de reserva: estado del pago, mensaje para el usuario y la reserva actualizada. */
public record ResultadoPagoResponse(
        Long idPago,
        EstadoPago estadoPago,
        String nroTransaccion,
        String mensaje,
        ReservaResponse reserva
) {}
