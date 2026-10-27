package com.patatrek.backend.guia.dto;

import com.patatrek.backend.certificado.EstadoCertificado;
import com.patatrek.backend.reserva.EstadoAsistencia;
import com.patatrek.backend.reserva.EstadoRequisitos;

import java.util.List;

/**
 * Participante de la nómina (CU-22), con lo que el guía necesita para el control (CU-15):
 * - certificado: estado del certificado más reciente (null si no cargó ninguno)
 * - equipamientoSolicitado: ids de ítems que su reserva pidió prestados a la empresa
 */
public record ParticipanteGuiaResponse(
        Long idParticipante,
        String nombre,
        String apellido,
        String dni,
        String telefono,
        String correoElectronico,
        Long idReserva,
        EstadoAsistencia estadoAsistencia,
        EstadoRequisitos estadoRequisitos,
        CertificadoResumen certificado,
        List<Long> equipamientoSolicitado
) {
    public record CertificadoResumen(EstadoCertificado estado) {
    }
}
