package com.patatrek.backend.reserva.dto;

import com.patatrek.backend.usuario.Cliente;

import java.time.LocalDate;

public record ClienteActualResponse(Long idUsuario, String nombre, String apellido, String correoElectronico,
                                    String dni, String telefono, LocalDate fechaNacimiento) {
    public static ClienteActualResponse de(Cliente c) {
        return new ClienteActualResponse(c.getIdUsuario(), c.getNombre(), c.getApellido(),
                c.getCorreoElectronico(), c.getDni(), c.getTelefono(), c.getFechaNacimiento());
    }
}
