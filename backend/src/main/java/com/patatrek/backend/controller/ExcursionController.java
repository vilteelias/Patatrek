package com.patatrek.backend.controller;

import com.patatrek.backend.excursion.ExcursionService;
import com.patatrek.backend.excursion.dto.ExcursionPublicaResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catálogo público (Visitante y Cliente). Solo muestra excursiones activas (RF-05).
 */
@RestController
@RequestMapping("/api/excursiones")
public class ExcursionController {

    private final ExcursionService excursionService;

    public ExcursionController(ExcursionService excursionService) {
        this.excursionService = excursionService;
    }

    @GetMapping
    public List<ExcursionPublicaResponse> listar() {
        return excursionService.listarPublicas();
    }

    @GetMapping("/{id}")
    public ExcursionPublicaResponse obtener(@PathVariable Long id) {
        return excursionService.obtenerPublica(id);
    }
}
