package com.patatrek.backend.certificado;

import com.patatrek.backend.reserva.Participante;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CERTIFICADO MEDICO = id_certificado_medico(PK) + id_participante(FK) + archivo_adjunto + fecha_subida
 *                    + fecha_vencimiento + estado_certificado + motivo_rechazo + intentos_envio + maximo_intentos
 * La carga (CU-24) y la validación (CU-25) llegan en sus módulos; acá se usa para el control de asistencia.
 */
@Entity
@Table(name = "certificado_medico")
public class CertificadoMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCertificadoMedico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_participante", nullable = false)
    private Participante participante;

    /** Ruta del archivo (máximo 5 MB, RNF-05). */
    @Column(nullable = false, length = 255)
    private String archivoAdjunto;

    @Column(nullable = false)
    private LocalDateTime fechaSubida;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoCertificado estadoCertificado;

    @Column(length = 500)
    private String motivoRechazo;

    @Column(nullable = false)
    private int intentosEnvio = 1;

    @Column(nullable = false)
    private int maximoIntentos = 3;

    public CertificadoMedico() {}

    public CertificadoMedico(Participante participante, String archivoAdjunto, LocalDateTime fechaSubida,
                             LocalDate fechaVencimiento, EstadoCertificado estadoCertificado) {
        this.participante = participante;
        this.archivoAdjunto = archivoAdjunto;
        this.fechaSubida = fechaSubida;
        this.fechaVencimiento = fechaVencimiento;
        this.estadoCertificado = estadoCertificado;
    }

    public Long getIdCertificadoMedico() { return idCertificadoMedico; }
    public void setIdCertificadoMedico(Long idCertificadoMedico) { this.idCertificadoMedico = idCertificadoMedico; }

    public Participante getParticipante() { return participante; }
    public void setParticipante(Participante participante) { this.participante = participante; }

    public String getArchivoAdjunto() { return archivoAdjunto; }
    public void setArchivoAdjunto(String archivoAdjunto) { this.archivoAdjunto = archivoAdjunto; }

    public LocalDateTime getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(LocalDateTime fechaSubida) { this.fechaSubida = fechaSubida; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public EstadoCertificado getEstadoCertificado() { return estadoCertificado; }
    public void setEstadoCertificado(EstadoCertificado estadoCertificado) { this.estadoCertificado = estadoCertificado; }

    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }

    public int getIntentosEnvio() { return intentosEnvio; }
    public void setIntentosEnvio(int intentosEnvio) { this.intentosEnvio = intentosEnvio; }

    public int getMaximoIntentos() { return maximoIntentos; }
    public void setMaximoIntentos(int maximoIntentos) { this.maximoIntentos = maximoIntentos; }
}
