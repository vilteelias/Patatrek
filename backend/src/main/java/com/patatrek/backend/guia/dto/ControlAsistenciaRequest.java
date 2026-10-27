package com.patatrek.backend.guia.dto;

import com.patatrek.backend.reserva.EstadoAsistencia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** CU-15: asistencia y equipamiento presentado de cada participante. */
public record ControlAsistenciaRequest(
        @NotNull(message = "Faltan los registros de asistencia")
        List<@Valid @NotNull RegistroAsistencia> registros
) {
    public record RegistroAsistencia(
            @NotNull(message = "Falta el participante") Long idParticipante,
            @NotNull(message = "Indicá si el participante está presente o ausente") EstadoAsistencia estadoAsistencia,
            List<Long> equipamientoPresentado
    ) {
    }
}
