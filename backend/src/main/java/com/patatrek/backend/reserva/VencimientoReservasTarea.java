package com.patatrek.backend.reserva;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Libera automáticamente los recursos de las reservas "Pendiente de pago" cuyo plazo
 * de 10 minutos venció (CU-18, flujo alternativo 14a). Se ejecuta cada 30 segundos.
 */
@Component
public class VencimientoReservasTarea {

    private final ReservaService reservaService;

    public VencimientoReservasTarea(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @Scheduled(fixedDelayString = "${patatrek.revision-vencimientos-ms:30000}", initialDelay = 10000)
    public void liberarVencidas() {
        reservaService.liberarVencidas();
    }
}
