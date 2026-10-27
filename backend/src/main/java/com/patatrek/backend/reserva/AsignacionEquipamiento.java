package com.patatrek.backend.reserva;

import com.patatrek.backend.equipamiento.Equipamiento;
import jakarta.persistence.*;

/**
 * ASIGNACION_EQUIPAMIENTO = id_equipamiento(PK) + id_reserva(PK) + cantidad_solicitada
 *                         + estado_asignacion (NUEVO: corrección de recursos retenidos)
 * Unidades de un ítem obligatorio que la reserva pide prestadas a la empresa (RF-15).
 */
@Entity
@Table(name = "asignacion_equipamiento")
public class AsignacionEquipamiento {

    @EmbeddedId
    private AsignacionEquipamientoId id;

    @MapsId("idEquipamiento")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_equipamiento")
    private Equipamiento equipamiento;

    @MapsId("idReserva")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_reserva")
    private Reserva reserva;

    @Column(nullable = false)
    private int cantidadSolicitada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoAsignacion estadoAsignacion = EstadoAsignacion.RETENIDO;

    protected AsignacionEquipamiento() {}

    public AsignacionEquipamiento(Equipamiento equipamiento, Reserva reserva, int cantidadSolicitada,
                                  EstadoAsignacion estadoAsignacion) {
        this.id = new AsignacionEquipamientoId(equipamiento.getIdEquipamiento(), reserva.getIdReserva());
        this.equipamiento = equipamiento;
        this.reserva = reserva;
        this.cantidadSolicitada = cantidadSolicitada;
        this.estadoAsignacion = estadoAsignacion;
    }

    public AsignacionEquipamientoId getId() { return id; }
    public Equipamiento getEquipamiento() { return equipamiento; }
    public Reserva getReserva() { return reserva; }

    public int getCantidadSolicitada() { return cantidadSolicitada; }
    public void setCantidadSolicitada(int cantidadSolicitada) { this.cantidadSolicitada = cantidadSolicitada; }

    public EstadoAsignacion getEstadoAsignacion() { return estadoAsignacion; }
    public void setEstadoAsignacion(EstadoAsignacion estadoAsignacion) { this.estadoAsignacion = estadoAsignacion; }
}
