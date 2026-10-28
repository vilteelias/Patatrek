package com.patatrek.backend.excepcion;

/** Correo o contraseña incorrectos, o sesión requerida (HTTP 401). */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
