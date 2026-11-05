package com.patatrek.backend.excursion.dto;

import com.patatrek.backend.excursion.Dificultad;
import com.patatrek.backend.excursion.Excursion;

import java.math.BigDecimal;

/**
 * Lo que ven Visitante y Cliente en el catálogo (RF-05):
 * nombre, descripciones general y técnica, dificultad, condiciones, precio base, imagen y recorrido base.
 */
public record ExcursionPublicaResponse(
        Long idExcursion,
        String nombre,
        String descripcionGeneral,
        String descripcionTecnica,
        Dificultad dificultad,
        Integer duracion,
        BigDecimal precio,
        boolean requiereCertificado,
        boolean aceptaMenores,
        String imagenPromocional,
        String recorridoBase
) {
    public static ExcursionPublicaResponse de(Excursion e) {
        return new ExcursionPublicaResponse(
                e.getIdExcursion(), e.getNombre(), e.getDescripcionGeneral(), e.getDescripcionTecnica(), e.getDificultad(),
                e.getDuracion(), e.getPrecio(), e.isRequiereCertificado(), e.isAceptaMenores(),
                e.getImagenPromocional(), e.getRecorridoBase());
    }
}
