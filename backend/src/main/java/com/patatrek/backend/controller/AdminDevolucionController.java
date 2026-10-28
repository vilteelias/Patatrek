package com.patatrek.backend.controller;

import com.patatrek.backend.devolucion.DevolucionService;
import com.patatrek.backend.devolucion.dto.DevolucionesResponse;
import com.patatrek.backend.devolucion.dto.ResultadoDevolucionResponse;
import org.springframework.web.bind.annotation.*;

/** Rol Administrador — CU-27 Registrar devolución de reserva (RF-24). */
@RestController
@RequestMapping("/api/admin/devoluciones")
public class AdminDevolucionController {

    private final DevolucionService service;

    public AdminDevolucionController(DevolucionService service) {
        this.service = service;
    }

    /** Reservas "Pendiente de devolución" (con el total a reintegrar) y las ya devueltas. */
    @GetMapping
    public DevolucionesResponse listar() {
        return service.listar();
    }

    /** Ejecuta el reembolso total de lo abonado a través de la pasarela simulada. */
    @PostMapping("/{idReserva}")
    public ResultadoDevolucionResponse devolver(@PathVariable Long idReserva) {
        return service.devolver(idReserva);
    }
}
