package com.patatrek.backend.certificado.dto;

import com.patatrek.backend.certificado.CertificadoMedico;
import com.patatrek.backend.certificado.EstadoCertificado;
import com.patatrek.backend.reserva.Participante;

import java.time.LocalDateTime;

/** Una fila de la pantalla "Certificados médicos" del Cliente / Participante (CU-24). */
public record CertificadoParticipanteResponse(
        Long idCertificadoMedico,
        String nombre,
        String apellido,
        boolean esTitular,
        EstadoCertificado estadoCertificado,
        LocalDateTime fechaSubida,
        String motivoRechazo,
        int intentosEnvio
) {
    public static CertificadoParticipanteResponse de(CertificadoMedico c, Participante p, boolean esTitular) {
        return new CertificadoParticipanteResponse(
                c.getIdCertificadoMedico(), p.getNombre(), p.getApellido(), esTitular,
                c.getEstadoCertificado(), c.getFechaSubida(), c.getMotivoRechazo(),
                c.getIntentosEnvio() == null ? 0 : c.getIntentosEnvio());
    }
}
