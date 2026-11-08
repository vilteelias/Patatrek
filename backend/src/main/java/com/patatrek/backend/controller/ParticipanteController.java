package com.patatrek.backend.controller;

import com.patatrek.backend.reserva.AccesoParticipanteService;
import com.patatrek.backend.reserva.dto.PagoParticipanteRequest;
import com.patatrek.backend.reserva.dto.ParticipanteReservaResponse;
import com.patatrek.backend.reserva.dto.ResultadoPagoParticipanteResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * CU-19 del lado del Participante, sin cuenta: acceso con ID de reserva + DNI.
 * Es público (como el enlace de valoración): la identificación es el DNI dentro de la reserva.
 */
@RestController
@RequestMapping("/api/participante")
public class ParticipanteController {

    private final AccesoParticipanteService service;

    public ParticipanteController(AccesoParticipanteService service) {
        this.service = service;
    }

    @GetMapping("/reservas/{id}")
    public ParticipanteReservaResponse consultar(@PathVariable Long id, @RequestParam(required = false) String dni) {
        return service.consultar(id, dni);
    }

    /** CU-26 4b: el participante paga el saldo restante. */
    @PostMapping("/reservas/{id}/pagos")
    public ResultadoPagoParticipanteResponse pagar(@PathVariable Long id,
                                                   @Valid @RequestBody PagoParticipanteRequest datos) {
        return service.pagarSaldo(id, datos);
    }
}
