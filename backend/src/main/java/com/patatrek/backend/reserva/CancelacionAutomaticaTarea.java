package com.patatrek.backend.reserva;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Temporizador del CU-30: dispara la verificación diaria de las reservas próximas a vencer.
 * Por defecto todos los días a las 03:00 (patatrek.cancelacion-automatica.cron).
 * El administrador también puede ejecutarla a mano desde Reservas (útil para mostrarlo).
 */
@Component
public class CancelacionAutomaticaTarea {

    private static final Logger log = LoggerFactory.getLogger(CancelacionAutomaticaTarea.class);

    private final CancelacionAutomaticaService service;

    public CancelacionAutomaticaTarea(CancelacionAutomaticaService service) {
        this.service = service;
    }

    @Scheduled(cron = "${patatrek.cancelacion-automatica.cron:0 0 3 * * *}")
    public void ejecutar() {
        var resultado = service.ejecutar();
        log.info("CU-30 (temporizador): {} salidas y {} reservas revisadas. {}",
                resultado.salidasRevisadas(), resultado.reservasRevisadas(), resultado.mensaje());
    }
}
