package com.patatrek.backend.pago;

/** Toda invocación a la pasarela genera un pago que empieza PENDIENTE y pasa a APROBADO o RECHAZADO (CU-26). */
public enum EstadoPago {
    PENDIENTE, APROBADO, RECHAZADO, DEVUELTO
}
