package com.patatrek.backend.contingencia;

/** RF-12: tipo de evento imprevisto. */
public enum TipoContingencia {
    CLIMATICA("climática"), OPERATIVA("operativa");

    private final String texto;

    TipoContingencia(String texto) { this.texto = texto; }

    public String texto() { return texto; }
}
