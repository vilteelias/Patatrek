package com.patatrek.backend.excepcion;

import java.util.Map;

/**
 * Datos obligatorios incompletos o con formato inválido detectados en el servicio (HTTP 400).
 * Las claves de "errores" indican el campo, por ejemplo "participantes[1].dni",
 * para que el formulario marque exactamente qué campo falta o está mal.
 */
public class DatosInvalidosException extends RuntimeException {

    private final Map<String, String> errores;

    public DatosInvalidosException(String mensaje, Map<String, String> errores) {
        super(mensaje);
        this.errores = errores;
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}
