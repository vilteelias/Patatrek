package com.patatrek.backend.contingencia;

import com.patatrek.backend.salida.Salida;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contingencia")
public class Contingencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contingencia")
    private Long idContingencia;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "tipo_contingencia", nullable = false, length = 50)
    private String tipoContingencia;

    @Column(name = "tipo_resolucion", nullable = false, length = 50)
    private String tipoResolucion;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(name = "fecha_anterior_salida")
    private LocalDateTime fechaAnteriorSalida;

    @Column(name = "recorrido_alternativo", length = 1000)
    private String recorridoAlternativo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_salida")
    private Salida salida;

    public Contingencia() {
        this.fechaRegistro = LocalDateTime.now();
    }

    public Contingencia(String tipoContingencia, String motivo, String tipoResolucion) {
        this.tipoContingencia = tipoContingencia;
        this.motivo = motivo;
        this.tipoResolucion = tipoResolucion;
        this.fechaRegistro = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getIdContingencia() { return idContingencia; }
    public void setIdContingencia(Long idContingencia) { this.idContingencia = idContingencia; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getTipoContingencia() { return tipoContingencia; }
    public void setTipoContingencia(String tipoContingencia) { this.tipoContingencia = tipoContingencia; }

    public String getTipoResolucion() { return tipoResolucion; }
    public void setTipoResolucion(String tipoResolucion) { this.tipoResolucion = tipoResolucion; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public LocalDateTime getFechaAnteriorSalida() { return fechaAnteriorSalida; }
    public void setFechaAnteriorSalida(LocalDateTime fechaAnteriorSalida) { this.fechaAnteriorSalida = fechaAnteriorSalida; }

    public String getRecorridoAlternativo() { return recorridoAlternativo; }
    public void setRecorridoAlternativo(String recorridoAlternativo) { this.recorridoAlternativo = recorridoAlternativo; }

    public Salida getSalida() { return salida; }
    public void setSalida(Salida salida) { this.salida = salida; }
}
