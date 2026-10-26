package com.patatrek.backend.certificadoMedico.dto;

import com.patatrek.backend.certificadoMedico.modelo.EstadoCertificado;
import com.patatrek.backend.certificadoMedico.modelo.CertificadoMedico;

import java.time.LocalDate;
import java.time.LocalDateTime;

//lo que se envia como respuesta despues de procesar el formulario
public record CertificadoMedicoResponse(
        Long idCertificadoMedico,
        String pathArchivoAdjunto,
        LocalDateTime fechaSubida,
        LocalDate fechaVencimiento,
        EstadoCertificado estadoCertificado,
        String motivoRechazo,
        Integer intentosEnvio

        //respuesta para Cargar Certificado CU-24
        //LocalDateTime horarioLimite,
        //String nombreParticipante,
        //String apellidoParticipante

) {
    // DTO (clase) a JSON
    public static CertificadoMedicoResponse de(CertificadoMedico c) {
        return new CertificadoMedicoResponse(
                c.getIdCertificadoMedico(), c.getPathArchivoAdjunto(), c.getFechaSubida(),
                c.getFechaVencimiento(), c.getEstadoCertificado(), c.getMotivoRechazo(),
                c.getIntentosEnvio());
    }
}
