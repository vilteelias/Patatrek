package com.patatrek.backend.salida;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class PlanificarSalidaDTO {
    private Long idExcursion;
    private LocalDate fechaInicio;
    private LocalTime horaInicio;
    private LocalDate fechaFinalizacion;
    private LocalTime horaFinalizacion;
    private Long idGuiaResponsable;
    private Integer cupoMaximo;
    private List<NocheRefugioDTO> refugiosNoches;

    public PlanificarSalidaDTO() {}

    public Long getIdExcursion() { return idExcursion; }
    public void setIdExcursion(Long idExcursion) { this.idExcursion = idExcursion; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalDate getFechaFinalizacion() { return fechaFinalizacion; }
    public void setFechaFinalizacion(LocalDate fechaFinalizacion) { this.fechaFinalizacion = fechaFinalizacion; }

    public LocalTime getHoraFinalizacion() { return horaFinalizacion; }
    public void setHoraFinalizacion(LocalTime horaFinalizacion) { this.horaFinalizacion = horaFinalizacion; }

    public Long getIdGuiaResponsable() { return idGuiaResponsable; }
    public void setIdGuiaResponsable(Long idGuiaResponsable) { this.idGuiaResponsable = idGuiaResponsable; }

    public Integer getCupoMaximo() { return cupoMaximo; }
    public void setCupoMaximo(Integer cupoMaximo) { this.cupoMaximo = cupoMaximo; }

    public List<NocheRefugioDTO> getRefugiosNoches() { return refugiosNoches; }
    public void setRefugiosNoches(List<NocheRefugioDTO> refugiosNoches) { this.refugiosNoches = refugiosNoches; }
}
