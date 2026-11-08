package com.patatrek.backend.reserva.dto;

import com.patatrek.backend.pago.EstadoPago;

/** Resultado del pago del saldo hecho por un participante, con su vista de la reserva actualizada. */
public record ResultadoPagoParticipanteResponse(
        Long idPago,
        EstadoPago estadoPago,
        String nroTransaccion,
        String mensaje,
        ParticipanteReservaResponse reserva
) {}
