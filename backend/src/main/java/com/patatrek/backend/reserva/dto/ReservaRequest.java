package com.patatrek.backend.reserva.dto;

import com.patatrek.backend.reserva.ModalidadAlojamiento;

import java.time.LocalDate;
import java.util.List;

/**
 * Lo que envía el asistente de reserva (CU-18) al confirmar el resumen.
 * Los campos se validan en ReservaService para poder indicar el participante exacto
 * (por ejemplo "participantes[1].dni") y mostrar el error al lado del campo obligatorio.
 */
public record ReservaRequest(
        Long idSalida,
        List<ParticipanteRequest> participantes,
        ModalidadAlojamiento modalidadAlojamiento,
        List<EquipamientoSolicitado> equipamiento
) {
    public record ParticipanteRequest(
            String dni,
            String nombre,
            String apellido,
            String correoElectronico,
            String nroTelefono,
            LocalDate fechaNacimiento
    ) {}

    /** Unidades de un ítem obligatorio que se piden prestadas (0 = cada participante lleva el suyo). */
    public record EquipamientoSolicitado(Long idEquipamiento, Integer cantidad) {}
}
