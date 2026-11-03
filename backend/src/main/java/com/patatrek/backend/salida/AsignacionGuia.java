package com.patatrek.backend.salida;

import com.patatrek.backend.usuario.Guia;
import jakarta.persistence.*;

/**
 * ASIGNACION_GUIA = id_guia(PK) + id_salida(PK) + tipo_guia
 * Cada salida tiene un guía responsable y, opcionalmente, guías ayudantes.
 */
@Entity
@Table(name = "asignacion_guia")
public class AsignacionGuia {

    @EmbeddedId
    private AsignacionGuiaId id;

    @MapsId("idGuia")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_guia")
    private Guia guia;

    @MapsId("idSalida")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_salida")
    private Salida salida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoGuia tipoGuia;

    protected AsignacionGuia() {}

    public AsignacionGuia(Guia guia, Salida salida, TipoGuia tipoGuia) {
        this.id = new AsignacionGuiaId(guia.getIdUsuario(), salida.getIdSalida());
        this.guia = guia;
        this.salida = salida;
        this.tipoGuia = tipoGuia;
    }

    public boolean esResponsable() {
        return tipoGuia == TipoGuia.RESPONSABLE;
    }

    public AsignacionGuiaId getId() { return id; }
    public Guia getGuia() { return guia; }
    public Salida getSalida() { return salida; }
    public TipoGuia getTipoGuia() { return tipoGuia; }
    public void setTipoGuia(TipoGuia tipoGuia) { this.tipoGuia = tipoGuia; }
}
