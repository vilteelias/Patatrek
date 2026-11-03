package com.patatrek.backend.soporte;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/** Arma cuerpos JSON simples para las pruebas (mapas, listas, textos, números y booleanos). */
public final class Json {

    private Json() {}

    public static String de(Object valor) {
        if (valor == null) return "null";
        if (valor instanceof Number || valor instanceof Boolean) return valor.toString();
        if (valor instanceof Map<?, ?> m) {
            return m.entrySet().stream().map(e -> de(String.valueOf(e.getKey())) + ":" + de(e.getValue()))
                    .collect(Collectors.joining(",", "{", "}"));
        }
        if (valor instanceof Collection<?> c) {
            return c.stream().map(Json::de).collect(Collectors.joining(",", "[", "]"));
        }
        String s = valor.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "\"" + s + "\"";
    }
}
