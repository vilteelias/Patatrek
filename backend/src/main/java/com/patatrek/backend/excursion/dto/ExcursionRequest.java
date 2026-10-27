package com.patatrek.backend.excursion.dto;

import com.patatrek.backend.excursion.Dificultad;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datos que envía el administrador al registrar (CU-04) o modificar (CU-07) una excursión.
 * Todos los campos son obligatorios salvo la lista de equipamiento,
 * que solo es obligatoria si requiereEquipamiento = true (se valida en el servicio).
 */
public record ExcursionRequest(

        @NotBlank(message = "Ingresá el nombre de la excursión")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "Ingresá la descripción general")
        @Size(max = 1000, message = "La descripción general no puede superar los 1000 caracteres")
        String descripcionGeneral,

        @NotBlank(message = "Ingresá la descripción técnica")
        @Size(max = 1000, message = "La descripción técnica no puede superar los 1000 caracteres")
        String descripcionTecnica,

        @NotNull(message = "Seleccioná el nivel de dificultad")
        Dificultad dificultad,

        @NotNull(message = "Ingresá la duración en días")
        @Min(value = 1, message = "La duración mínima es 1 día")
        @Max(value = 30, message = "La duración máxima es 30 días")
        Integer duracion,

        @NotNull(message = "Ingresá el precio base")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "Precio inválido")
        BigDecimal precio,

        @NotBlank(message = "Ingresá el recorrido base")
        @Size(max = 1000, message = "El recorrido no puede superar los 1000 caracteres")
        String recorridoBase,

        @NotNull(message = "Indicá si requiere certificado médico")
        Boolean requiereCertificado,

        @NotNull(message = "Indicá si admite menores de edad")
        Boolean aceptaMenores,

        @NotNull(message = "Indicá si requiere equipamiento obligatorio")
        Boolean requiereEquipamiento,

        List<Long> equipamientoIds
) {
}
