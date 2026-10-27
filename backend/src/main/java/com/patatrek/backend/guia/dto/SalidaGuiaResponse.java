package com.patatrek.backend.guia.dto;

import com.patatrek.backend.equipamiento.EquipamientoResponse;
import com.patatrek.backend.salida.EstadoSalida;
import com.patatrek.backend.salida.TipoGuia;

import java.time.LocalDate;
import java.util.List;

/**
 * Salida vista por el guía (CU-10). Incluye lo que necesita el control de asistencia (CU-15):
 * si la excursión requiere certificado y su equipamiento obligatorio.
 * horaInicio / horaFinalizacion con formato "HH:mm".
 */
public record SalidaGuiaResponse(
        Long idSalida,
        String nombreExcursion,
        LocalDate fechaInicio,
        LocalDate fechaFinalizacion,
        String horaInicio,
        String horaFinalizacion,
        EstadoSalida estadoSalida,
        TipoGuia rolGuia,
        int cupoMaximo,
        int cuposOcupados,
        String guiaResponsable,
        List<String> guiasAyudantes,
        List<String> refugios,
        String observaciones,
        boolean requiereCertificado,
        List<EquipamientoResponse> equipamientoObligatorio
) {
}
