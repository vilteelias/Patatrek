package com.patatrek.backend.salida.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "salida")
public class Salida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_excursion", nullable = false)
    private Long idExcursion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column (name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(name = "guia_responsable")
    private String guiaResponsable;

    @Column(name = "cupo_maximo", nullable = false)
    private Integer cupoMaximo;

    @Column(name = "cupo_disponible", nullable = false)
    private Integer cupoDisponible;

    @Column(nullable = false)
    private String estado = "DISPONIBLE";

    @Column(name = "capacidad_refugio")
    private Integer capacidadRefugio;

    private String refugio;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public Integer getCupoDisponible() { return cupoDisponible; }
    public void setCupoDisponible(Integer cupoDisponible) { this.cupoDisponible = cupoDisponible; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Integer getCapacidadRefugio() { return capacidadRefugio; }
    public void setCapacidadRefugio(Integer capacidadRefugio) { this.capacidadRefugio = capacidadRefugio; }
    
    public String getRefugio() { return refugio; }
    public void setRefugio(String refugio) { this.refugio = refugio; }
}