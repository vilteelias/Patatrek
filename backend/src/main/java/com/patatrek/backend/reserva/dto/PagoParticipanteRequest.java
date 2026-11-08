package com.patatrek.backend.reserva.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Pago del saldo restante hecho por un participante (CU-26 4b). Se identifica con su DNI.
 * "aprobar" se ignora salvo en las pruebas automáticas (igual que en PagoRequest).
 */
public record PagoParticipanteRequest(
        @NotBlank(message = "Ingresá tu DNI.") String dni,
        Boolean aprobar
) {}
