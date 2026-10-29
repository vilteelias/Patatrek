package com.patatrek.backend.certificado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de PUT /api/admin/certificados/{id}/rechazar. El motivo es obligatorio: se envía al cliente por correo. */
public record RechazoRequest(
        @NotBlank(message = "Ingresá el motivo del rechazo.")
        @Size(max = 500, message = "El motivo no puede superar los 500 caracteres.")
        String motivo
) {
}
