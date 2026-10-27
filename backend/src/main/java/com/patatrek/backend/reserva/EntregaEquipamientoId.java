package com.patatrek.backend.reserva;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta de ENTREGA_EQUIPAMIENTO: (id_equipamiento, id_participante). */
@Embeddable
public class EntregaEquipamientoId implements Serializable {

    private Long idEquipamiento;
    private Long idParticipante;

    protected EntregaEquipamientoId() {}

    public EntregaEquipamientoId(Long idEquipamiento, Long idParticipante) {
        this.idEquipamiento = idEquipamiento;
        this.idParticipante = idParticipante;
    }

    public Long getIdEquipamiento() { return idEquipamiento; }
    public Long getIdParticipante() { return idParticipante; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntregaEquipamientoId otro)) return false;
        return Objects.equals(idEquipamiento, otro.idEquipamiento) && Objects.equals(idParticipante, otro.idParticipante);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idEquipamiento, idParticipante);
    }
}
