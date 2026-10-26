package com.patatrek.backend.salida.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class PlanificarSalidaDTO {
    private Long idExcursion;
    private LocalDate fechaInicio;
    private LocalTime horaInicio;
    private LocalDate fechaFin;
    private LocalTime horaFin;
    private String guiaResponsable;
    private Integer cupoMaximo;
    private String refugio;
    private Integer capacidadRefugio;

    // Getters y Setters
    public Long getIdExcursion() { return idExcursion; }
    public void setIdExcursion(Long idExcursion) { this.idExcursion = idExcursion; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

    public String getGuiaResponsable() { return guiaResponsable; }
    public void setGuiaResponsable(String guiaResponsable) { this.guiaResponsable = guiaResponsable; }

    public Integer getCupoMaximo() { return cupoMaximo; }
    public void setCupoMaximo(Integer cupoMaximo) { this.cupoMaximo = cupoMaximo; }

    public String getRefugio() { return refugio; }
    public void setRefugio(String refugio) { this.refugio = refugio; }

    public Integer getCapacidadRefugio() { return capacidadRefugio; }
    public void setCapacidadRefugio(Integer capacidadRefugio) { this.capacidadRefugio = capacidadRefugio; }
    
}
