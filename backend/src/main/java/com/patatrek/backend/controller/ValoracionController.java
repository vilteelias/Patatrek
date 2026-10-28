package com.patatrek.backend.controller;

import com.patatrek.backend.valoracion.ValoracionService;
import com.patatrek.backend.valoracion.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CU-28 Registrar valoración de excursión (Cliente / Participante) y
 * CU-29 Consultar valoraciones de excursiones (Administrador).
 */
@RestController
public class ValoracionController {

    private final ValoracionService service;

    public ValoracionController(ValoracionService service) {
        this.service = service;
    }

    /** Panel del cliente: salidas finalizadas que todavía no valoró. */
    @GetMapping("/api/cliente/valoraciones-pendientes")
    public List<ValoracionPendienteResponse> pendientes() {
        return service.pendientesDelCliente();
    }

    /** Participante (con o sin cuenta): se identifica con el ID de reserva y su DNI (enlace del correo). */
    @GetMapping("/api/valoraciones/formulario")
    public FormularioValoracionResponse formulario(@RequestParam Long reserva, @RequestParam String dni) {
        return service.formulario(reserva, dni);
    }

    @PostMapping("/api/valoraciones")
    @ResponseStatus(HttpStatus.CREATED)
    public FormularioValoracionResponse registrar(@RequestBody ValoracionRequest datos) {
        return service.registrar(datos);
    }

    @GetMapping("/api/admin/valoraciones")
    public List<ResumenValoracionesResponse> resumen() {
        return service.resumen();
    }

    @GetMapping("/api/admin/valoraciones/excursiones/{idExcursion}")
    public DetalleValoracionesResponse detalle(@PathVariable Long idExcursion) {
        return service.detalle(idExcursion);
    }
}
