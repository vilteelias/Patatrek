package com.patatrek.backend.usuario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guias")
@CrossOrigin(origins = "http://localhost:5173")
public class GuiaAdminController {

    @Autowired
    private GuiaRepository guiaRepository;

    @GetMapping
    public ResponseEntity<List<Guia>> listarGuias() {
        return ResponseEntity.ok(guiaRepository.findAll());
    }
}
