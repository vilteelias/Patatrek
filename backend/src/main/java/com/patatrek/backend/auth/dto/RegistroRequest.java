package com.patatrek.backend.auth.dto;

import java.time.LocalDate;

/** CU-01 Registrar cliente (RF-01). Todos los campos son obligatorios. */
public record RegistroRequest(
        String nombre,
        String apellido,
        String dni,
        String correoElectronico,
        String telefono,
        LocalDate fechaNacimiento,
        String contrasenia,
        String confirmarContrasenia
) {}
