package com.patatrek.backend.controller;

import com.patatrek.backend.excursion.EstadoExcursion;
import com.patatrek.backend.excursion.ExcursionService;
import com.patatrek.backend.excursion.dto.BajaExcursionRequest;
import com.patatrek.backend.excursion.dto.ExcursionAdminResponse;
import com.patatrek.backend.excursion.dto.ExcursionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Gestión del catálogo por parte del Administrador (CU-04, CU-05, CU-06, CU-07, CU-08).
 * Todas las rutas /api/admin/** quedarán protegidas por rol en el módulo de Autenticación.
 */
@RestController
@RequestMapping("/api/admin/excursiones")
public class AdminExcursionController {

    private final ExcursionService excursionService;

    public AdminExcursionController(ExcursionService excursionService) {
        this.excursionService = excursionService;
    }

    /** Catálogo completo. ?estado=ACTIVA o ?estado=INACTIVA para filtrar. */
    @GetMapping
    public List<ExcursionAdminResponse> listar(@RequestParam(required = false) EstadoExcursion estado) {
        return excursionService.listarAdmin(estado);
    }

    @GetMapping("/{id}")
    public ExcursionAdminResponse obtener(@PathVariable Long id) {
        return excursionService.obtenerAdmin(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExcursionAdminResponse registrar(@Valid @RequestBody ExcursionRequest datos) {
        return excursionService.registrar(datos);
    }

    @PutMapping("/{id}")
    public ExcursionAdminResponse modificar(@PathVariable Long id, @Valid @RequestBody ExcursionRequest datos) {
        return excursionService.modificar(id, datos);
    }

    @PatchMapping("/{id}/baja")
    public ExcursionAdminResponse darDeBaja(@PathVariable Long id, @Valid @RequestBody BajaExcursionRequest datos) {
        return excursionService.darDeBaja(id, datos.motivo());
    }

    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExcursionAdminResponse subirImagen(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo) {
        return excursionService.actualizarImagen(id, archivo);
    }
}
