package com.patatrek.backend.salida;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/refugios")
@CrossOrigin(origins = "http://localhost:5173")
public class RefugioController {

    @Autowired
    private RefugioRepository refugioRepository;

    @GetMapping
    public ResponseEntity<List<Refugio>> listarRefugios() {
        return ResponseEntity.ok(refugioRepository.findAll());
    }
}
