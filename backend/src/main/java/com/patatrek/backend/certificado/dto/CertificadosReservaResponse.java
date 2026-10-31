package com.patatrek.backend.certificado.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de GET /api/reservas/{id}/certificados.
 * horarioLimite: último momento para cargar o reemplazar certificados (72 hs antes de la salida).
 */
public record CertificadosReservaResponse(
        LocalDateTime horarioLimite,
        List<CertificadoParticipanteResponse> certificados
) {
}
