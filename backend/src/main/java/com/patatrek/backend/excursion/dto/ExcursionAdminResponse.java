package com.patatrek.backend.excursion.dto;

import com.patatrek.backend.equipamiento.EquipamientoResponse;
import com.patatrek.backend.excursion.Dificultad;
import com.patatrek.backend.excursion.EstadoExcursion;
import com.patatrek.backend.excursion.Excursion;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vista completa para el Administrador.
 * tieneSalidasActivas = true indica que los parámetros operativos están bloqueados (RF-06).
 */
public record ExcursionAdminResponse(
        Long idExcursion,
        String nombre,
        String descripcionGeneral,
        String descripcionTecnica,
        Dificultad dificultad,
        Integer duracion,
        BigDecimal precio,
        String recorridoBase,
        boolean requiereCertificado,
        boolean aceptaMenores,
        boolean requiereEquipamiento,
        List<EquipamientoResponse> equipamientoObligatorio,
        EstadoExcursion estadoExcursion,
        String motivoBaja,
        String imagenPromocional,
        boolean tieneSalidasActivas
) {
    public static ExcursionAdminResponse de(Excursion e, boolean tieneSalidasActivas) {
        return new ExcursionAdminResponse(
                e.getIdExcursion(), e.getNombre(), e.getDescripcionGeneral(), e.getDescripcionTecnica(),
                e.getDificultad(), e.getDuracion(), e.getPrecio(), e.getRecorridoBase(),
                e.isRequiereCertificado(), e.isAceptaMenores(), e.isRequiereEquipamiento(),
                e.getEquipamientoObligatorio().stream().map(EquipamientoResponse::de).toList(),
                e.getEstadoExcursion(), e.getMotivoBaja(), e.getImagenPromocional(),
                tieneSalidasActivas);
    }
}
