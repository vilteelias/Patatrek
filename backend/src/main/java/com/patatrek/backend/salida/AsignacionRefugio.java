package com.patatrek.backend.salida;

import jakarta.persistence.*;

/**
 * ASIGNACION_REFUGIO = id_refugio(PK) + id_salida(PK) + orden_visita + plazas_reservadas + plazas_ocupadas
 *                    + plazas_retenidas (NUEVO: corrección de recursos retenidos)
 */
@Entity
@Table(name = "asignacion_refugio")
public class AsignacionRefugio {

    @EmbeddedId
    private AsignacionRefugioId id;

    @MapsId("idRefugio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_refugio")
    private Refugio refugio;

    @MapsId("idSalida")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_salida")
    private Salida salida;

    @Column(nullable = false)
    private int ordenVisita;

    /** Plazas aseguradas para la salida. */
    @Column(nullable = false)
    private int plazasReservadas;

    /** Plazas consolidadas por reservas pagadas. */
    @Column(nullable = false)
    private int plazasOcupadas;

    /** NUEVO: plazas apartadas durante los 10 minutos de pago (RF-15). */
    @Column(nullable = false)
    private int plazasRetenidas;

    protected AsignacionRefugio() {}

    public AsignacionRefugio(Refugio refugio, Salida salida, int ordenVisita, int plazasReservadas) {
        this.id = new AsignacionRefugioId(refugio.getIdRefugio(), salida.getIdSalida());
        this.refugio = refugio;
        this.salida = salida;
        this.ordenVisita = ordenVisita;
        this.plazasReservadas = plazasReservadas;
    }

    public AsignacionRefugioId getId() { return id; }
    public Refugio getRefugio() { return refugio; }
    public Salida getSalida() { return salida; }

    public int getOrdenVisita() { return ordenVisita; }
    public void setOrdenVisita(int ordenVisita) { this.ordenVisita = ordenVisita; }

    public int getPlazasReservadas() { return plazasReservadas; }
    public void setPlazasReservadas(int plazasReservadas) { this.plazasReservadas = plazasReservadas; }

    public int getPlazasOcupadas() { return plazasOcupadas; }
    public void setPlazasOcupadas(int plazasOcupadas) { this.plazasOcupadas = plazasOcupadas; }

    public int getPlazasRetenidas() { return plazasRetenidas; }
    public void setPlazasRetenidas(int plazasRetenidas) { this.plazasRetenidas = plazasRetenidas; }
}
