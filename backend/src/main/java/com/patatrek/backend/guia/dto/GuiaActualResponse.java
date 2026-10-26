package com.patatrek.backend.guia.dto;

import com.patatrek.backend.usuario.Guia;

public record GuiaActualResponse(Long idGuia, String nombre, String apellido) {

    public static GuiaActualResponse de(Guia g) {
        return new GuiaActualResponse(g.getIdUsuario(), g.getNombre(), g.getApellido());
    }
}
