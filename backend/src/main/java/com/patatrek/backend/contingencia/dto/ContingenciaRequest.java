package com.patatrek.backend.contingencia.dto;

import com.patatrek.backend.contingencia.TipoContingencia;
import com.patatrek.backend.contingencia.TipoResolucion;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * CU-14 Registrar contingencia de salida (RF-12).
 * Reprogramación: nueva fecha y hora de inicio (la salida conserva su duración; la hora de finalización es opcional)
 * y, si también cambia, el recorrido alternativo. Modificación: solo el recorrido alternativo.
 */
public record ContingenciaRequest(
        TipoContingencia tipoContingencia,
        TipoResolucion resolucion,
        String motivo,
        LocalDate nuevaFechaInicio,
        LocalTime nuevaHoraInicio,
        LocalTime nuevaHoraFinalizacion,
        String nuevoRecorrido
) {}
