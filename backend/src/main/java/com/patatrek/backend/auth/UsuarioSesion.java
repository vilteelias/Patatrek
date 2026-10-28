package com.patatrek.backend.auth;

import java.io.Serializable;

/** Usuario que inició sesión. Se guarda en la sesión HTTP (por eso es Serializable). */
public record UsuarioSesion(Long id, Rol rol, String nombre, String apellido, String correoElectronico, String dni)
        implements Serializable {
}
