package com.patatrek.backend.salida;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta de ASIGNACION_REFUGIO: (id_refugio, id_salida). */
@Embeddable
public class AsignacionRefugioId implements Serializable {

    private Long idRefugio;
    private Long idSalida;

    protected AsignacionRefugioId() {}

    public AsignacionRefugioId(Long idRefugio, Long idSalida) {
        this.idRefugio = idRefugio;
        this.idSalida = idSalida;
    }

    public Long getIdRefugio() { return idRefugio; }
    public Long getIdSalida() { return idSalida; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AsignacionRefugioId otro)) return false;
        return Objects.equals(idRefugio, otro.idRefugio) && Objects.equals(idSalida, otro.idSalida);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idRefugio, idSalida);
    }
}
