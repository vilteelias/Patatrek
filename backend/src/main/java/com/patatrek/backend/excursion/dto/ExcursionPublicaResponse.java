package com.patatrek.backend.excursion.dto;

import com.patatrek.backend.excursion.Dificultad;
import com.patatrek.backend.excursion.Excursion;

import java.math.BigDecimal;

/**
 * Lo que ven Visitante y Cliente en el catálogo (RF-05):
 * nombre, descripción, dificultad, condiciones, precio base e imagen.
 */
public record ExcursionPublicaResponse(
        Long idExcursion,
        String nombre,
        String descripcionGeneral,
        Dificultad dificultad,
        Integer duracion,
        BigDecimal precio,
        boolean requiereCertificado,
        boolean aceptaMenores,
        String imagenPromocional
) {
    public static ExcursionPublicaResponse de(Excursion e) {
        return new ExcursionPublicaResponse(
                e.getIdExcursion(), e.getNombre(), e.getDescripcionGeneral(), e.getDificultad(),
                e.getDuracion(), e.getPrecio(), e.isRequiereCertificado(), e.isAceptaMenores(),
                e.getImagenPromocional());
    }
}
