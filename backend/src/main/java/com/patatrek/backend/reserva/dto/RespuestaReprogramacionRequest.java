package com.patatrek.backend.reserva.dto;

/** RF-12: true = acepta la nueva fecha; false = la rechaza (la reserva pasa a Pendiente de devolución). */
public record RespuestaReprogramacionRequest(Boolean acepta) {}
