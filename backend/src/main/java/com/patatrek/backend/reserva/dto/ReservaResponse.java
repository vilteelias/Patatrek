package com.patatrek.backend.reserva.dto;

import com.patatrek.backend.pago.EstadoPago;
import com.patatrek.backend.pago.ModoPago;
import com.patatrek.backend.reserva.EstadoAsignacion;
import com.patatrek.backend.reserva.EstadoReserva;
import com.patatrek.backend.reserva.ModalidadAlojamiento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Detalle de una reserva: resumen, tiempo de retención restante, recursos y pagos. */
public record ReservaResponse(
        Long idReserva,
        EstadoReserva estadoReserva,
        LocalDateTime fechaHora,
        int cantidadParticipantes,
        BigDecimal montoTotal,
        BigDecimal importeSena,
        BigDecimal saldoPendiente,
        ModalidadAlojamiento modalidadAlojamiento,
        LocalDateTime fechaHoraVencimientoRetencion,
        long segundosParaVencer,
        String motivoCancelacion,
        List<ModoPago> modosPagoDisponibles,
        String motivoNoPuedePagar,
        SalidaResumen salida,
        List<ParticipanteResumen> participantes,
        List<RefugioResumen> refugios,
        List<EquipamientoResumen> equipamiento,
        List<PagoResumen> pagos
) {
    public record SalidaResumen(Long idSalida, Long idExcursion, String excursion, LocalDate fechaInicio,
                                LocalDate fechaFinalizacion, LocalTime horaInicio, LocalTime horaFinalizacion,
                                BigDecimal precioPorPersona) {}

    public record ParticipanteResumen(String dni, String nombre, String apellido, String correoElectronico) {}

    public record RefugioResumen(String nombre, BigDecimal precioNoche) {}

    public record EquipamientoResumen(String nombre, int cantidad, EstadoAsignacion estado) {}

    public record PagoResumen(Long idPago, LocalDateTime fechaPago, BigDecimal importe, ModoPago modoPago,
                              EstadoPago estadoPago, String nroTransaccion) {}
}
