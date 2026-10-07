package com.patatrek.backend.excursion.dto;

import com.patatrek.backend.excursion.modelo.Dificultad;
import java.math.BigDecimal;


//esta clase solo transporta datos entre el frontend y el backend
//evita exponer directamente las entidades de la base de datos
//son los datos que el administrador completa en el formulario al registrar la excursion
public record ExcursionRequest(
        String nombre,
        String descripcionGeneral,
        String descripcionTecnica,
        Integer duracion,
        String recorridoBase,
        BigDecimal precio,
        String imagenPromocional,
        Dificultad dificultad,
        Boolean aceptaMenores,
        Boolean requiereCertificado,
        Boolean requiereEquipamiento
    )
{ }
