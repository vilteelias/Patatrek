package com.patatrek.backend.controller;

import com.patatrek.backend.salida.EstadoSalida;
import com.patatrek.backend.salida.GestionSalidaService;
import com.patatrek.backend.salida.dto.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Rol Administrador — Gestión de salidas: CU-09 Planificar, CU-10 Listar, CU-11 Modificar,
 * CU-12 Asignar personal y cupos y CU-13 Dar de baja. Las contingencias (CU-14) están en AdminContingenciaController.
 */
@RestController
@RequestMapping("/api/admin/salidas")
public class AdminSalidaController {

    private final GestionSalidaService service;

    public AdminSalidaController(GestionSalidaService service) {
        this.service = service;
    }

    /** Catálogo general de salidas. Filtros opcionales: nombre de la excursión y estado. */
    @GetMapping
    public List<SalidaAdminResponse> listar(@RequestParam(required = false) String excursion,
                                            @RequestParam(required = false) EstadoSalida estado) {
        return service.listar(excursion, estado);
    }

    /**
     * Excursiones activas, guías y refugios. Con el período (fechas y horas), cada guía dice si está libre
     * y cada refugio sus plazas libres. excluirSalida: la salida que se está modificando.
     */
    @GetMapping("/opciones")
    public OpcionesSalidaResponse opciones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFinalizacion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFinalizacion,
            @RequestParam(required = false) Long excluirSalida) {
        return service.opciones(fechaInicio, horaInicio, fechaFinalizacion, horaFinalizacion, excluirSalida);
    }

    @GetMapping("/{id}")
    public SalidaAdminResponse detalle(@PathVariable Long id) {
        return service.detalle(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalidaAdminResponse planificar(@RequestBody PlanificarSalidaRequest datos) {
        return service.planificar(datos);
    }

    @PutMapping("/{id}")
    public SalidaAdminResponse modificar(@PathVariable Long id, @RequestBody ModificarSalidaRequest datos) {
        return service.modificar(id, datos);
    }

    @PutMapping("/{id}/ayudantes")
    public SalidaAdminResponse asignarAyudantes(@PathVariable Long id, @RequestBody AyudantesRequest datos) {
        return service.asignarAyudantes(id, datos);
    }

    @PostMapping("/{id}/baja")
    public SalidaAdminResponse darDeBaja(@PathVariable Long id, @Valid @RequestBody BajaSalidaRequest datos) {
        return service.darDeBaja(id, datos.motivo());
    }
}
