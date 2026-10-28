package com.patatrek.backend.equipamiento.dto;

import com.patatrek.backend.salida.EstadoSalida;

import java.time.LocalDate;

/** Salidas que se pueden elegir en la consulta de equipamiento. */
public record SalidaEquipamientoResponse(Long idSalida, String excursion, LocalDate fechaInicio,
                                         LocalDate fechaFinalizacion, EstadoSalida estadoSalida) {}
