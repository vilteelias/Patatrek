package com.patatrek.backend.certificadoMedico.modelo;

import com.patatrek.backend.participante.modelo.Participante;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificado_medico")
public class CertificadoMedico {

    // atributo estático para el límite de intentos (no se guarda en la base de datos)
    public static final int MAX_INTENTOS = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_certificado_medico")
    private Long idCertificadoMedico;

    @Column(name = "path_archivo_adjunto")
    private String pathArchivoAdjunto;

    @Column(name = "fecha_subida")
    private LocalDateTime fechaSubida;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_certificado")
    private EstadoCertificado estadoCertificado;

    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    @Column(name = "intentos_envio")
    private Integer intentosEnvio;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_participante")
    private Participante participante;

    // Constructor sin argumentos (JPA)
    public CertificadoMedico() {
        this.fechaSubida = LocalDateTime.now();
        this.intentosEnvio = 0;
    }

    // Lógica de Negocio
    public void incrementarIntentos() {
        if (this.intentosEnvio == null) {
            this.intentosEnvio = 0;
        }
        this.intentosEnvio++;
    }

    public boolean alcanzoMaximoIntentos() {
        return this.intentosEnvio != null && this.intentosEnvio >= MAX_INTENTOS;
    }

    // Getters y Setters
    public Long getIdCertificadoMedico() {
        return idCertificadoMedico;
    }

    public void setIdCertificadoMedico(Long idCertificadoMedico) {
        this.idCertificadoMedico = idCertificadoMedico;
    }

    public String getPathArchivoAdjunto() {
        return pathArchivoAdjunto;
    }

    public void setPathArchivoAdjunto(String pathArchivoAdjunto) {
        this.pathArchivoAdjunto = pathArchivoAdjunto;
    }

    public LocalDateTime getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(LocalDateTime fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public EstadoCertificado getEstadoCertificado() {
        return estadoCertificado;
    }

    public void setEstadoCertificado(EstadoCertificado estadoCertificado) { this.estadoCertificado = estadoCertificado; }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public Integer getIntentosEnvio() {
        return intentosEnvio;
    }

    public void setIntentosEnvio(Integer intentosEnvio) {
        this.intentosEnvio = intentosEnvio;
    }

    public Participante getParticipante() {
        return participante;
    }

    public void setParticipante(Participante participante) {
        this.participante = participante;
    }
}