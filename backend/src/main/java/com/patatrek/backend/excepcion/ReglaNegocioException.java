package com.patatrek.backend.excepcion;

import java.util.Map;

/**
 * Violación de una regla de negocio (HTTP 409).
 * Opcionalmente indica el campo afectado para marcarlo en el formulario.
 */
public class ReglaNegocioException extends RuntimeException {

    private final Map<String, String> errores;

    public ReglaNegocioException(String mensaje) {
        this(mensaje, Map.of());
    }

    public ReglaNegocioException(String mensaje, Map<String, String> errores) {
        super(mensaje);
        this.errores = errores;
    }

    public static ReglaNegocioException enCampo(String campo, String mensaje) {
        return new ReglaNegocioException(mensaje, Map.of(campo, mensaje));
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}
