package com.patatrek.backend.reserva.dto;

import jakarta.validation.constraints.Size;

/** CU-23: el motivo es opcional (flujo 3.b: si no se ingresa, se registra que no se cargó). */
public record CancelacionRequest(
        @Size(max = 400, message = "El motivo puede tener hasta 400 caracteres.") String motivo
) {}
