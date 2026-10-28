package com.patatrek.backend.valoracion.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Detalle de valoraciones de una excursión (CU-29): promedio, total, cantidad por estrella y comentarios. */
public record DetalleValoracionesResponse(
        Long idExcursion,
        String excursion,
        Double promedio,
        long total,
        List<Long> cantidadPorEstrellas,
        List<Item> valoraciones
) {
    /** cantidadPorEstrellas.get(0) = cantidad de 1 estrella … get(4) = cantidad de 5 estrellas. */
    public record Item(Long idValoracion, int calificacion, String comentario, LocalDateTime fecha,
                       String participante, Long idSalida, LocalDate fechaSalida) {}
}
