package com.patatrek.backend.controller;

import com.patatrek.backend.guia.OperacionTerrenoService;
import com.patatrek.backend.guia.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Rol Guía (paquete "Operación en terreno"): CU-10, CU-22, CU-15 y CU-16.
 * Las rutas /api/guia/** quedarán protegidas por rol en el módulo de Autenticación.
 */
@RestController
@RequestMapping("/api/guia")
public class PanelGuiaController {

    private final OperacionTerrenoService service;

    public PanelGuiaController(OperacionTerrenoService service) {
        this.service = service;
    }

    @GetMapping("/yo")
    public GuiaActualResponse guiaActual() {
        return GuiaActualResponse.de(service.guiaActual());
    }

    @GetMapping("/salidas")
    public List<SalidaGuiaResponse> misSalidas() {
        return service.misSalidas();
    }

    @GetMapping("/salidas/{id}")
    public SalidaGuiaResponse salida(@PathVariable Long id) {
        return service.detalleSalida(id);
    }

    @GetMapping("/salidas/{id}/participantes")
    public List<ParticipanteGuiaResponse> participantes(@PathVariable Long id) {
        return service.participantes(id);
    }

    @PostMapping("/salidas/{id}/control-asistencia")
    public SalidaGuiaResponse controlAsistencia(@PathVariable Long id, @Valid @RequestBody ControlAsistenciaRequest datos) {
        return service.registrarControl(id, datos.registros());
    }

    @PostMapping("/salidas/{id}/finalizacion")
    public FinalizacionResponse finalizar(@PathVariable Long id, @Valid @RequestBody FinalizacionRequest datos) {
        return service.finalizar(id, datos.observaciones());
    }
}
