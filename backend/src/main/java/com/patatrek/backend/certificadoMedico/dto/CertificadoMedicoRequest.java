package com.patatrek.backend.certificadoMedico.dto;

import com.patatrek.backend.certificadoMedico.modelo.EstadoCertificado;
import java.time.LocalDate;

//lo que envia el cliente a traves del formulario
public record CertificadoMedicoRequest(
        Long idReserva,
        String dni,
        Long idCertificadoMedico,
        String pathArchivoAdjunto,
        LocalDate fechaVencimiento,
        EstadoCertificado estadoCertificado,
        String motivoRechazo
) {}
