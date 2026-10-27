package com.patatrek.backend.reserva.dto;

import com.patatrek.backend.pago.ModoPago;
import jakarta.validation.constraints.NotNull;

/**
 * Confirmación del pago desde la pantalla de la pasarela simulada (CU-26).
 * NO incluye datos de tarjeta: la pasarela los pide y los procesa por su cuenta.
 * "aprobar" solo existe porque la pasarela es simulada: permite mostrar en la demo
 * el caso aprobado y el caso rechazado (flujo alternativo 7a).
 */
public record PagoRequest(
        @NotNull(message = "Elegí cómo querés pagar.") ModoPago modoPago,
        @NotNull(message = "Indicá la respuesta simulada de la pasarela.") Boolean aprobar
) {}
