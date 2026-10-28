package com.patatrek.backend.controller;

import com.patatrek.backend.equipamiento.StockEquipamientoService;
import com.patatrek.backend.equipamiento.dto.SalidaEquipamientoResponse;
import com.patatrek.backend.equipamiento.dto.StockEquipamientoResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Rol Administrador — consulta de disponibilidad del equipamiento (solo lectura). */
@RestController
@RequestMapping("/api/admin/equipamiento")
public class AdminStockEquipamientoController {

    private final StockEquipamientoService service;

    public AdminStockEquipamientoController(StockEquipamientoService service) {
        this.service = service;
    }

    /** Stock de cada ítem; con idSalida, la disponibilidad para las fechas de esa salida. */
    @GetMapping("/stock")
    public StockEquipamientoResponse stock(@RequestParam(required = false) Long idSalida) {
        return service.stock(idSalida);
    }

    @GetMapping("/salidas")
    public List<SalidaEquipamientoResponse> salidas() {
        return service.salidas();
    }
}
