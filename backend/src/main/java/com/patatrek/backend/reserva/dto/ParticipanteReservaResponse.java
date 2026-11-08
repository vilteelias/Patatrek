package com.patatrek.backend.reserva.dto;

import com.patatrek.backend.pago.ModoPago;
import com.patatrek.backend.reserva.EstadoReserva;

import java.math.BigDecimal;
import java.util.List;

/**
 * CU-19 vista del Participante (acceso con ID de reserva + DNI): el estado de la reserva, la fecha, el saldo
 * pendiente y la lista de participantes. No incluye datos personales de los demás (DNI, correo).
 * El participante solo puede pagar el saldo restante (CU-26, flujo 4b).
 */
public record ParticipanteReservaResponse(
        Long idReserva,
        EstadoReserva estadoReserva,
        String titular,
        ParticipanteActual participante,
        int cantidadParticipantes,
        BigDecimal montoTotal,
        BigDecimal importeSena,
        BigDecimal saldoPendiente,
        List<ModoPago> modosPagoDisponibles,
        String motivoNoPuedePagar,
        ReservaResponse.SalidaResumen salida,
        List<String> participantes,
        String motivoCancelacion
) {
    public record ParticipanteActual(String dni, String nombre, String apellido) {}
}
