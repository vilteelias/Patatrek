package com.patatrek.backend.reserva.dto;

import com.patatrek.backend.reserva.EstadoReserva;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fila de "Mis reservas" del cliente. */
public record ReservaClienteResponse(Long idReserva, String excursion, LocalDate fechaInicio,
                                     LocalDate fechaFinalizacion, EstadoReserva estadoReserva,
                                     int cantidadParticipantes, BigDecimal montoTotal, BigDecimal saldoPendiente) {}
