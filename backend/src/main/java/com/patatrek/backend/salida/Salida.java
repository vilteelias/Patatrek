package com.patatrek.backend.salida;

import com.patatrek.backend.excursion.Excursion;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SALIDA = id_salida(PK) + id_excursion(FK) + fecha_inicio + fecha_finalizacion + hora_inicio
 *        + hora_finalizacion + fecha_hora_fin_real + cupo_base + cupo_maximo + estado_salida
 *        + observaciones + motivo_cancelacion + recorrido + cupos_ocupados + cupo_total
 *        + cupos_retenidos (NUEVO: corrección de recursos retenidos)
 *
 * Por ahora solo se usa desde Excursiones para saber si hay salidas activas.
 * La lógica completa (planificar, listar, modificar...) llega en el módulo Salidas.
 */
@Entity
@Table(name = "salida")
public class Salida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idSalida;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_excursion", nullable = false)
    private Excursion excursion;

    @OneToMany(mappedBy = "salida", cascade = CascadeType.ALL, orphanRemoval = true)
private List<AsignacionGuia> asignacionGuias = new ArrayList<>();

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFinalizacion;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFinalizacion;

    private LocalDateTime fechaHoraFinReal;

    @Column(nullable = false)
    private int cupoBase;

    @Column(nullable = false)
    private int cupoMaximo;

    @Column(nullable = false)
    private int cupoTotal;

    /** Cupos consolidados (reservas Señadas o Confirmadas). */
    @Column(nullable = false)
    private int cuposOcupados;

    /** NUEVO: cupos apartados por reservas "Pendiente de Pago" durante los 10 minutos de retención. */
    @Column(nullable = false)
    private int cuposRetenidos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoSalida estadoSalida = EstadoSalida.DISPONIBLE;

    @Column(length = 1000)
    private String observaciones;

    @Column(length = 500)
    private String motivoCancelacion;

    @Column(length = 1000)
    private String recorrido;

    /** Cupos que todavía se pueden reservar: total - ocupados - retenidos. */
    public int getCuposDisponibles() {
        return cupoTotal - cuposOcupados - cuposRetenidos;
    }

    public Long getIdSalida() { return idSalida; }
    public void setIdSalida(Long idSalida) { this.idSalida = idSalida; }

    public Excursion getExcursion() { return excursion; }
    public void setExcursion(Excursion excursion) { this.excursion = excursion; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate dia) { this.fechaInicio = dia; }

    public LocalDate getFechaFinalizacion() { return fechaFinalizacion; }
    public void setFechaFinalizacion(LocalDate fechaFinalizacion) { this.fechaFinalizacion = fechaFinalizacion; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFinalizacion() { return horaFinalizacion; }
    public void setHoraFinalizacion(LocalTime horaFinalizacion) { this.horaFinalizacion = horaFinalizacion; }

    public LocalDateTime getFechaHoraFinReal() { return fechaHoraFinReal; }
    public void setFechaHoraFinReal(LocalDateTime fechaHoraFinReal) { this.fechaHoraFinReal = fechaHoraFinReal; }

    public int getCupoBase() { return cupoBase; }
    public void setCupoBase(int cupoBase) { this.cupoBase = cupoBase; }

    public int getCupoMaximo() { return cupoMaximo; }
    public void setCupoMaximo(int cupoMaximo) { this.cupoMaximo = cupoMaximo; }

    public int getCupoTotal() { return cupoTotal; }
    public void setCupoTotal(int cupoTotal) { this.cupoTotal = cupoTotal; }

    public int getCuposOcupados() { return cuposOcupados; }
    public void setCuposOcupados(int cuposOcupados) { this.cuposOcupados = cuposOcupados; }

    public int getCuposRetenidos() { return cuposRetenidos; }
    public void setCuposRetenidos(int cuposRetenidos) { this.cuposRetenidos = cuposRetenidos; }

    public EstadoSalida getEstadoSalida() { return estadoSalida; }
    public void setEstadoSalida(EstadoSalida estadoSalida) { this.estadoSalida = estadoSalida; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getMotivoCancelacion() { return motivoCancelacion; }
    public void setMotivoCancelacion(String motivoCancelacion) { this.motivoCancelacion = motivoCancelacion; }

    public String getRecorrido() { return recorrido; }
    public void setRecorrido(String recorrido) { this.recorrido = recorrido; }

    public void addAsignacionGuia(AsignacionGuia asignacionGuia) {
    if (this.asignacionGuias == null) {
        this.asignacionGuias = new ArrayList<>();
    }
    this.asignacionGuias.add(asignacionGuia);
}

public List<AsignacionGuia> getAsignacionGuias() {
    return asignacionGuias;
}

public void setAsignacionGuias(List<AsignacionGuia> asignacionGuias) {
    this.asignacionGuias = asignacionGuias;
}
}
