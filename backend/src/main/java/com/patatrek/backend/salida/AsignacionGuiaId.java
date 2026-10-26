package com.patatrek.backend.salida;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta de ASIGNACION_GUIA: (id_guia, id_salida). */
@Embeddable
public class AsignacionGuiaId implements Serializable {

    private Long idGuia;
    private Long idSalida;

    protected AsignacionGuiaId() {}

    public AsignacionGuiaId(Long idGuia, Long idSalida) {
        this.idGuia = idGuia;
        this.idSalida = idSalida;
    }

    public Long getIdGuia() { return idGuia; }
    public Long getIdSalida() { return idSalida; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AsignacionGuiaId otro)) return false;
        return Objects.equals(idGuia, otro.idGuia) && Objects.equals(idSalida, otro.idSalida);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idGuia, idSalida);
    }
}
