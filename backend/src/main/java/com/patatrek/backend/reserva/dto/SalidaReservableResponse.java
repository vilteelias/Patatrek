package com.patatrek.backend.reserva.dto;

import com.patatrek.backend.salida.EstadoSalida;

import java.time.LocalDate;
import java.time.LocalTime;

/** Salida de una excursión tal como la ve el cliente antes de reservar (CU-10 vista cliente / CU-18 paso 1). */
public record SalidaReservableResponse(
        Long idSalida,
        LocalDate fechaInicio,
        LocalDate fechaFinalizacion,
        LocalTime horaInicio,
        LocalTime horaFinalizacion,
        int cuposDisponibles,
        EstadoSalida estadoSalida,
        boolean tieneRefugio,
        boolean reservable,
        String motivoNoReservable
) {}
