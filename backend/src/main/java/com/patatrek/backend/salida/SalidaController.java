package com.patatrek.backend.salida;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salidas")
@CrossOrigin(origins = "http://localhost:5173")
public class SalidaController {

    @Autowired
    private SalidaService salidaService;

    @GetMapping
    public ResponseEntity<List<Salida>> listarSalidas() {
        return ResponseEntity.ok(salidaService.obtenerTodas());
    }

    

   @GetMapping("/{id}")
    public ResponseEntity<Salida> obtenerSalidaPorId(@PathVariable Long id) {
        Salida salida = salidaService.obtenerPorId(id);
        return ResponseEntity.ok(salida);
    }

    @PostMapping
    public ResponseEntity<Salida> planificarSalida(@RequestBody PlanificarSalidaDTO dto) {
        Salida salidaCreada = salidaService.planificarSalida(dto);
        return ResponseEntity.ok(salidaCreada);
    }

    @PutMapping("/{id}/ayudantes")
    public ResponseEntity<Salida> asignarAyudantes(
            @PathVariable Long id,
            @RequestBody AsignacionAyudantesDTO dto) {
        Salida salidaActualizada = salidaService.asignarAyudantesYRecalcularCupos(id, dto);
        return ResponseEntity.ok(salidaActualizada);
    }
}
