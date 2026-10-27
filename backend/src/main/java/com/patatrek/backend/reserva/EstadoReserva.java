package com.patatrek.backend.reserva;

import java.util.EnumSet;
import java.util.Set;

/** Estados de una reserva (RF-15, RF-12, RF-20). */
public enum EstadoReserva {
    PENDIENTE_PAGO, SENADA, CONFIRMADA, CANCELADA, PENDIENTE_DEVOLUCION, DEVUELTA;

    /** Reservas pagadas (seña o total): sus participantes forman la nómina de la salida. */
    public static final Set<EstadoReserva> VIGENTES = EnumSet.of(SENADA, CONFIRMADA);
}
