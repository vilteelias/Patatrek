package com.patatrek.backend.reserva;

import com.patatrek.backend.equipamiento.Equipamiento;
import jakarta.persistence.*;

/**
 * ENTREGA_EQUIPAMIENTO = id_equipamiento(PK) + id_participante(PK) + cantidad_entregada + entregado
 * Lo registra el guía en el control de asistencia (CU-15):
 *  - entregado: el participante cuenta con el ítem obligatorio (propio o prestado).
 *  - cantidad_entregada: unidades que le prestó la empresa (0 si trajo el suyo).
 */
@Entity
@Table(name = "entrega_equipamiento")
public class EntregaEquipamiento {

    @EmbeddedId
    private EntregaEquipamientoId id;

    @MapsId("idEquipamiento")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_equipamiento")
    private Equipamiento equipamiento;

    @MapsId("idParticipante")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_participante")
    private Participante participante;

    @Column(nullable = false)
    private int cantidadEntregada;

    @Column(nullable = false)
    private boolean entregado;

    protected EntregaEquipamiento() {}

    public EntregaEquipamiento(Equipamiento equipamiento, Participante participante, int cantidadEntregada,
                               boolean entregado) {
        this.id = new EntregaEquipamientoId(equipamiento.getIdEquipamiento(), participante.getIdParticipante());
        this.equipamiento = equipamiento;
        this.participante = participante;
        this.cantidadEntregada = cantidadEntregada;
        this.entregado = entregado;
    }

    public EntregaEquipamientoId getId() { return id; }
    public Equipamiento getEquipamiento() { return equipamiento; }
    public Participante getParticipante() { return participante; }

    public int getCantidadEntregada() { return cantidadEntregada; }
    public void setCantidadEntregada(int cantidadEntregada) { this.cantidadEntregada = cantidadEntregada; }

    public boolean isEntregado() { return entregado; }
    public void setEntregado(boolean entregado) { this.entregado = entregado; }
}
