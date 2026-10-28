package com.patatrek.backend.valoracion.dto;

import java.time.LocalDate;

/** Valoración pendiente que el cliente ve en su panel (CU-28 paso 1). */
public record ValoracionPendienteResponse(Long idReserva, String dni, String excursion,
                                          LocalDate fechaInicio, LocalDate fechaFinalizacion) {}
