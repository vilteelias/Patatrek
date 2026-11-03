package com.patatrek.backend.reserva.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Resultado de una ejecución de CU-30 Cancelar reservas automáticamente. */
public record ResultadoCancelacionAutomaticaResponse(
        LocalDateTime ejecutadaEn,
        int salidasRevisadas,
        int reservasRevisadas,
        List<ReservaCancelada> canceladas,
        String mensaje
) {
    public record ReservaCancelada(Long idReserva, String cliente, String excursion, Long idSalida, String motivo) {}
}
