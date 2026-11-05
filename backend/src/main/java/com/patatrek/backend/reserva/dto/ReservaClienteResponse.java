package com.patatrek.backend.reserva.dto;

import com.patatrek.backend.reserva.EstadoReserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Fila de "Mis reservas" del cliente (CU-21 Listar reservas realizadas). */
public record ReservaClienteResponse(Long idReserva, String excursion, LocalDate fechaInicio,
                                     LocalDate fechaFinalizacion, LocalTime horaInicio, EstadoReserva estadoReserva,
                                     int cantidadParticipantes, BigDecimal montoTotal, BigDecimal saldoPendiente,
                                     String estadoPago, LocalDateTime fechaHora,
                                     boolean reprogramacionPendiente) {}
