package com.patatrek.backend.salida.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** CU-13 Dar de baja salida (RF-11): el motivo es obligatorio. */
public record BajaSalidaRequest(
        @NotBlank(message = "Ingresá el motivo de la baja.")
        @Size(max = 400, message = "El motivo puede tener hasta 400 caracteres.")
        String motivo
) {}
