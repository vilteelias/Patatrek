package com.patatrek.backend.salida;

import java.util.EnumSet;
import java.util.Set;

/** Estados de una salida según RF-09. */
public enum EstadoSalida {
    DISPONIBLE, COMPLETA, CANCELADA, EN_CURSO, FINALIZADA;

    /** Salidas "programadas activas": bloquean la edición de parámetros operativos (RF-06). */
    public static final Set<EstadoSalida> ACTIVAS = EnumSet.of(DISPONIBLE, COMPLETA, EN_CURSO);
}
