package com.patatrek.backend.controller;

import com.patatrek.backend.equipamiento.EquipamientoRepository;
import com.patatrek.backend.equipamiento.EquipamientoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Catálogo de equipamiento precargado, usado por el modal "Agregar equipamiento" (CU-05). */
@RestController
@RequestMapping("/api/admin/equipamiento")
public class AdminEquipamientoController {

    private final EquipamientoRepository equipamientoRepository;

    public AdminEquipamientoController(EquipamientoRepository equipamientoRepository) {
        this.equipamientoRepository = equipamientoRepository;
    }

    @GetMapping
    public List<EquipamientoResponse> listar() {
        return equipamientoRepository.findAllByOrderByNombreAsc().stream()
                .map(EquipamientoResponse::de).toList();
    }
}
