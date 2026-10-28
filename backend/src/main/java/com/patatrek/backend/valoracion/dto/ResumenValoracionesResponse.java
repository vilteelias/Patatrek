package com.patatrek.backend.valoracion.dto;

/** Una fila del listado de excursiones con su promedio y total de valoraciones (CU-29 / RF-26). */
public record ResumenValoracionesResponse(Long idExcursion, String excursion, boolean activa,
                                          Double promedio, long total) {}
