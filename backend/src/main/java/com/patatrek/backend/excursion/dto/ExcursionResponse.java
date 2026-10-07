package com.patatrek.backend.excursion.dto;

import com.patatrek.backend.excursion.modelo.Dificultad;
import com.patatrek.backend.excursion.modelo.EstadoExcursion;
import com.patatrek.backend.excursion.modelo.Excursion;

import java.math.BigDecimal;

//lo que el frontend recibe al listar o al registrar una excursion
public record ExcursionResponse(
        Long idExcursion,
        String nombre,
        String descripcionGeneral,
        String descripcionTecnica,
        Dificultad dificultad,
        Integer duracion,
        BigDecimal precio,
        boolean requiereCertificado,
        boolean requiereEquipamiento,
        boolean aceptaMenores,
        EstadoExcursion estadoExcursion,
        String imagenPromocional,
        String recorridoBase
    ) {
        //convierte la entidad en DTO
        public static ExcursionResponse de(Excursion e){
            return new ExcursionResponse(
                    e.getIdExcursion(), e.getNombre(), e.getDescripcionGeneral(), e.getDescripcionTecnica(), e.getDificultad(), e.getDuracion(), e.getPrecio(), e.isRequiereCertificado(), e.isRequiereEquipamiento(), e.isAceptaMenores(), e.getEstadoExcursion(), e.getImagenPromocional(), e.getRecorridoBase()
            );
    }
}