package com.patatrek.backend.reserva.dto;

import com.patatrek.backend.pago.ModoPago;
import jakarta.validation.constraints.NotNull;

/**
 * Confirmación del pago desde la pantalla de la pasarela simulada (CU-26).
 * NO incluye datos de tarjeta: la pasarela los pide y los procesa por su cuenta.
 * La pasarela decide al azar si aprueba o rechaza el pago. "aprobar" se ignora,
 * salvo en las pruebas automáticas (patatrek.pasarela.permitir-forzar-resultado=true), que necesitan un resultado fijo.
 */
public record PagoRequest(
        @NotNull(message = "Elegí cómo querés pagar.") ModoPago modoPago,
        Boolean aprobar
) {}
