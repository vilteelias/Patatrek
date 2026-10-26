package com.patatrek.backend.reserva;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta de ASIGNACION_EQUIPAMIENTO: (id_equipamiento, id_reserva). */
@Embeddable
public class AsignacionEquipamientoId implements Serializable {

    private Long idEquipamiento;
    private Long idReserva;

    protected AsignacionEquipamientoId() {}

    public AsignacionEquipamientoId(Long idEquipamiento, Long idReserva) {
        this.idEquipamiento = idEquipamiento;
        this.idReserva = idReserva;
    }

    public Long getIdEquipamiento() { return idEquipamiento; }
    public Long getIdReserva() { return idReserva; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AsignacionEquipamientoId otro)) return false;
        return Objects.equals(idEquipamiento, otro.idEquipamiento) && Objects.equals(idReserva, otro.idReserva);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idEquipamiento, idReserva);
    }
}
