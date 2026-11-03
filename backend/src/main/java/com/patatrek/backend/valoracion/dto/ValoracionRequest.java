package com.patatrek.backend.valoracion.dto;

/**
 * CU-28. El participante se identifica con el ID de reserva y su DNI.
 * La calificación se valida en el servicio para devolver el mensaje exacto del flujo 5a.
 */
public record ValoracionRequest(Long idReserva, String dni, Integer calificacion, String comentario) {}
