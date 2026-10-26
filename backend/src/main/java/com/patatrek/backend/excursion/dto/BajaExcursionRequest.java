package com.patatrek.backend.excursion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** CU-08: la baja lógica exige un motivo. */
public record BajaExcursionRequest(
        @NotBlank(message = "Ingresá el motivo de la baja")
        @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
        String motivo
) {
}
