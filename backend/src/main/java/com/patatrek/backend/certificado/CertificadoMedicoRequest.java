package com.patatrek.backend.certificado;

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
