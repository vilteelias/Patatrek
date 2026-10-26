package com.patatrek.backend.reserva;

/**
 * NUEVO (corrección de recursos retenidos): estado del equipamiento pedido por una reserva.
 * RETENIDO mientras se paga (10 minutos), CONSOLIDADO con el pago aprobado, LIBERADO si vence o se cancela.
 */
public enum EstadoAsignacion {
    RETENIDO, CONSOLIDADO, LIBERADO
}
