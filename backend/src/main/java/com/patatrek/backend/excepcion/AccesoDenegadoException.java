package com.patatrek.backend.excepcion;

/** El usuario no tiene permiso sobre el recurso. Se traduce a HTTP 403. */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
