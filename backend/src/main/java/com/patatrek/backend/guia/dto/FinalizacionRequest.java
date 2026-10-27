package com.patatrek.backend.guia.dto;

import jakarta.validation.constraints.Size;

/** CU-16: las observaciones son opcionales. */
public record FinalizacionRequest(
        @Size(max = 1000, message = "Las observaciones no pueden superar los 1000 caracteres")
        String observaciones
) {
}
