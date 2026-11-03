package com.patatrek.backend.controller;

import com.patatrek.backend.contingencia.ContingenciaService;
import com.patatrek.backend.contingencia.dto.ContingenciaRequest;
import com.patatrek.backend.contingencia.dto.ContingenciaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Rol Administrador — CU-14 Registrar contingencia de salida (RF-12). */
@RestController
@RequestMapping("/api/admin/salidas/{idSalida}/contingencias")
public class AdminContingenciaController {

    private final ContingenciaService service;

    public AdminContingenciaController(ContingenciaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContingenciaResponse registrar(@PathVariable Long idSalida, @RequestBody ContingenciaRequest datos) {
        return service.registrar(idSalida, datos);
    }
}
