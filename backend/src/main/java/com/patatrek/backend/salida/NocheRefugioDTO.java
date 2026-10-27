package com.patatrek.backend.salida;

public class NocheRefugioDTO {
    private Long idRefugio;
    private Integer ordenVisita;
    private Integer plazasReservadas;

    public NocheRefugioDTO() {}

    public NocheRefugioDTO(Long idRefugio, Integer ordenVisita, Integer plazasReservadas) {
        this.idRefugio = idRefugio;
        this.ordenVisita = ordenVisita;
        this.plazasReservadas = plazasReservadas;
    }

    public Long getIdRefugio() { return idRefugio; }
    public void setIdRefugio(Long idRefugio) { this.idRefugio = idRefugio; }

    public Integer getOrdenVisita() { return ordenVisita; }
    public void setOrdenVisita(Integer ordenVisita) { this.ordenVisita = ordenVisita; }

    public Integer getPlazasReservadas() { return plazasReservadas; }
    public void setPlazasReservadas(Integer plazasReservadas) { this.plazasReservadas = plazasReservadas; }
}
