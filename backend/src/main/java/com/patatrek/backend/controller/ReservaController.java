package com.patatrek.backend.controller;

import com.patatrek.backend.reserva.ReservaService;
import com.patatrek.backend.reserva.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Rol Cliente — paquete "Reservas y pagos": CU-18 Realizar reserva y CU-26 Realizar pago.
 * Las rutas quedarán protegidas por rol en el módulo de Autenticación.
 */
@RestController
public class ReservaController {

    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    /** Cliente con el que se usan las pantallas (TEMPORAL hasta Autenticación). */
    @GetMapping("/api/cliente/yo")
    public ClienteActualResponse clienteActual() {
        return ClienteActualResponse.de(service.clienteActual());
    }

    @GetMapping("/api/cliente/reservas")
    public List<ReservaClienteResponse> misReservas() {
        return service.misReservas();
    }

    /** Próximas salidas de una excursión, indicando si se pueden reservar y por qué no. */
    @GetMapping("/api/excursiones/{idExcursion}/salidas")
    public List<SalidaReservableResponse> salidas(@PathVariable Long idExcursion) {
        return service.salidasDeExcursion(idExcursion);
    }

    /** Datos que necesita el asistente de reserva (precio, refugios, equipamiento, cupos). */
    @GetMapping("/api/reservas/salidas/{idSalida}")
    public DatosReservaResponse datosParaReservar(@PathVariable Long idSalida) {
        return service.datosParaReservar(idSalida);
    }

    /** CU-18: registra la reserva "Pendiente de pago" y retiene los recursos 10 minutos. */
    @PostMapping("/api/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse crear(@RequestBody ReservaRequest datos) {
        return service.crear(datos);
    }

    @GetMapping("/api/reservas/{id}")
    public ReservaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /** CU-26: el cliente confirmó el pago en la pasarela (simulada). Sin datos de tarjeta. */
    @PostMapping("/api/reservas/{id}/pagos")
    public ResultadoPagoResponse pagar(@PathVariable Long id, @Valid @RequestBody PagoRequest datos) {
        return service.pagar(id, datos);
    }

    /** SOLO DEMO: fuerza el vencimiento de la retención para no esperar 10 minutos. */
    @PostMapping("/api/reservas/{id}/simular-vencimiento")
    public ReservaResponse simularVencimiento(@PathVariable Long id) {
        return service.simularVencimiento(id);
    }
}
