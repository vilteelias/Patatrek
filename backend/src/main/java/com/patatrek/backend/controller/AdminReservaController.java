package com.patatrek.backend.controller;

import com.patatrek.backend.reserva.EstadoReserva;
import com.patatrek.backend.reserva.ReservaService;
import com.patatrek.backend.reserva.dto.ReservaAdminResponse;
import com.patatrek.backend.reserva.dto.ReservaResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Rol Administrador — CU-20 Listar reservas generales (RF-17).
 * Las rutas /api/admin/** quedarán protegidas por rol en el módulo de Autenticación.
 */
@RestController
@RequestMapping("/api/admin/reservas")
public class AdminReservaController {

    private final ReservaService service;

    public AdminReservaController(ReservaService service) {
        this.service = service;
    }

    /** Todos los filtros son opcionales y se combinan entre sí. */
    @GetMapping
    public List<ReservaAdminResponse> listar(
            @RequestParam(required = false) String excursion,
            @RequestParam(required = false) Long idSalida,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) EstadoReserva estado) {
        return service.listarReservasGenerales(excursion, idSalida, desde, hasta, dni, estado);
    }

    /** Detalle de una reserva (participantes, equipamiento y pagos). */
    @GetMapping("/{id}")
    public ReservaResponse detalle(@PathVariable Long id) {
        return service.obtener(id);
    }
}
