package com.patatrek.backend.config;

import com.patatrek.backend.salida.GestionSalidaService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, corrige las salidas que quedaron con cupo total 0 (las planificadas con la versión anterior
 * de la pantalla no lo calculaban y aparecían sin cupos en el catálogo) y las que quedaron sin recorrido
 * (se completa con el recorrido base de su excursión).
 */
@Component
@Order(20)
public class ReparacionSalidas implements CommandLineRunner {

    private final GestionSalidaService service;

    public ReparacionSalidas(GestionSalidaService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        service.repararCuposTotales();
        service.repararRecorridos();
    }
}
