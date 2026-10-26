package com.patatrek.backend.salida.controlador;

import com.patatrek.backend.salida.dto.PlanificarSalidaDTO;
import com.patatrek.backend.salida.modelo.Salida;
import com.patatrek.backend.salida.servicio.SalidaServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salidas")
@CrossOrigin(origins = "http://localhost:5173")
public class SalidaControlador {

    @Autowired
    private SalidaServicio salidaServicio;

    @GetMapping
    public List<Salida> listarSalidas() {
        return salidaServicio.obtenerTodas();
    }

    @PostMapping
    public ResponseEntity<Salida> planificarSalida(@RequestBody PlanificarSalidaDTO dto) {
        Salida salidaCreada = salidaServicio.planificarSalida(dto);
        return ResponseEntity.ok(salidaCreada);
    }
}