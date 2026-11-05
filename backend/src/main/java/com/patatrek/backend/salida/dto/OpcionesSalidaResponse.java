package com.patatrek.backend.salida.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que necesitan las pantallas de planificar y modificar una salida: excursiones activas,
 * guías con su disponibilidad y refugios con sus plazas libres para el período consultado.
 */
public record OpcionesSalidaResponse(
        List<ExcursionOpcion> excursiones,
        List<GuiaOpcion> guias,
        List<RefugioOpcion> refugios
) {
    public record ExcursionOpcion(Long idExcursion, String nombre, int duracionDias, String recorridoBase) {}

    /** disponible = false si ya tiene otra salida que se superpone (motivo dice cuál). */
    public record GuiaOpcion(Long idGuia, String nombre, int capacidadEstandar, boolean disponible,
                             String motivoNoDisponible) {}

    /** plazasLibres = capacidad − plazas ya reservadas por otras salidas que se superponen en esas noches. */
    public record RefugioOpcion(Long idRefugio, String nombre, int capacidadPlazas, int plazasLibres,
                                BigDecimal precioNoche) {}
}
