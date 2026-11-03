package com.patatrek.backend.contingencia;

import java.time.LocalDate;
import java.time.LocalTime;

public class ContingenciaDTO {
    private String tipoContingencia; // Ej: "Climática", "Operativa"
    private String resolucion;       // Ej: "Cancelación", "Reprogramación", "Modificación"
    private String motivo;
    
    // Campos adicionales usados por Reprogramación y Modificación
    private LocalDate nuevaFecha;
    private LocalTime nuevoHorario;
    private String nuevoRecorrido;

    // Getters y Setters
    public String getTipoContingencia() { return tipoContingencia; }
    public void setTipoContingencia(String tipoContingencia) { this.tipoContingencia = tipoContingencia; }

    public String getResolucion() { return resolucion; }
    public void setResolucion(String resolucion) { this.resolucion = resolucion; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public LocalDate getNuevaFecha() { return nuevaFecha; }
    public void setNuevaFecha(LocalDate nuevaFecha) { this.nuevaFecha = nuevaFecha; }

    public LocalTime getNuevoHorario() { return nuevoHorario; }
    public void setNuevoHorario(LocalTime nuevoHorario) { this.nuevoHorario = nuevoHorario; }

    public String getNuevoRecorrido() { return nuevoRecorrido; }
    public void setNuevoRecorrido(String nuevoRecorrido) { this.nuevoRecorrido = nuevoRecorrido; }
}
