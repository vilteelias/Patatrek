package com.patatrek.backend.salida.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * CU-09 Planificar salida (RF-08). Para una salida de varios días, refugiosPorNoche tiene un refugio
 * por noche (fecha de finalización − fecha de inicio) y plazasPorRefugio son las plazas a reservar en cada uno.
 * Un mismo refugio puede repetirse en varias noches.
 */
public record PlanificarSalidaRequest(
        Long idExcursion,
        LocalDate fechaInicio,
        LocalTime horaInicio,
        LocalDate fechaFinalizacion,
        LocalTime horaFinalizacion,
        Long idGuiaResponsable,
        Integer cupoMaximo,
        Integer plazasPorRefugio,
        List<Long> refugiosPorNoche
) {}
