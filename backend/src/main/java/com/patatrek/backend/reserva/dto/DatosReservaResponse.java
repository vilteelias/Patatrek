package com.patatrek.backend.reserva.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Todo lo que necesita el asistente de reserva para una salida (CU-18). */
public record DatosReservaResponse(
        Long idSalida,
        Long idExcursion,
        String excursion,
        String imagenPromocional,
        String dificultad,
        BigDecimal precioPorPersona,
        boolean requiereCertificado,
        boolean aceptaMenores,
        LocalDate fechaInicio,
        LocalDate fechaFinalizacion,
        LocalTime horaInicio,
        LocalTime horaFinalizacion,
        boolean multiDia,
        int cuposDisponibles,
        int minutosRetencion,
        List<RefugioDisponible> refugios,
        int plazasRefugioDisponibles,
        BigDecimal alojamientoRefugioPorPersona,
        List<EquipamientoDisponible> equipamiento
) {
    public record RefugioDisponible(String nombre, int ordenVisita, BigDecimal precioNoche, int plazasDisponibles) {}

    public record EquipamientoDisponible(Long idEquipamiento, String nombre, String descripcion, int disponibles) {}
}
