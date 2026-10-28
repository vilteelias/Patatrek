package com.patatrek.backend.pago;

/**
 * Modos de pago (RF-23): seña del 50%, pago completo o saldo restante de una reserva señada.
 * REINTEGRO identifica la transacción de devolución total que genera el administrador (CU-27 / RF-24).
 */
public enum ModoPago {
    SENA, TOTAL, SALDO, REINTEGRO
}
