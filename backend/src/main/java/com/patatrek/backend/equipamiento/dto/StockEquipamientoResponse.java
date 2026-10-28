package com.patatrek.backend.equipamiento.dto;

import com.patatrek.backend.reserva.EstadoAsignacion;
import com.patatrek.backend.reserva.EstadoReserva;
import com.patatrek.backend.salida.EstadoSalida;

import java.time.LocalDate;
import java.util.List;

/**
 * Consulta de disponibilidad del equipamiento (ERS, "Gestión de equipamiento": controlar la disponibilidad
 * en tiempo real de cada ítem para una salida). Sin salida elegida, la disponibilidad es la de hoy.
 * disponible = stock total − retenido (reservas esperando el pago) − prestado (reservas pagadas en esas fechas).
 */
public record StockEquipamientoResponse(
        SalidaResumen salida,
        LocalDate desde,
        LocalDate hasta,
        List<Item> items,
        List<Asignacion> asignaciones
) {
    public record SalidaResumen(Long idSalida, String excursion, LocalDate fechaInicio, LocalDate fechaFinalizacion,
                                EstadoSalida estadoSalida) {}

    public record Item(Long idEquipamiento, String nombre, String descripcion, int stockTotal, int retenido,
                       int prestado, int disponible) {}

    /** Equipamiento pedido por cada reserva de la salida elegida. */
    public record Asignacion(Long idReserva, String cliente, EstadoReserva estadoReserva, String equipamiento,
                             int cantidad, EstadoAsignacion estadoAsignacion) {}
}
