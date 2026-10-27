package com.patatrek.backend.excepcion;

import java.util.Map;

/**
 * Formato único de error que recibe el frontend:
 * { "mensaje": "...", "errores": { "campo": "mensaje del campo" } }
 */
public record RespuestaError(String mensaje, Map<String, String> errores) {
}
