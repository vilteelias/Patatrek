package com.patatrek.backend.valoracion.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Datos del formulario de valoración (CU-28 paso 3). Si ya valoró, trae su valoración. */
public record FormularioValoracionResponse(
        Long idReserva,
        String dni,
        String participante,
        String excursion,
        LocalDate fechaInicio,
        LocalDate fechaFinalizacion,
        String acreditacion,
        boolean yaValorada,
        Integer calificacion,
        String comentario,
        LocalDateTime fechaValoracion
) {}
