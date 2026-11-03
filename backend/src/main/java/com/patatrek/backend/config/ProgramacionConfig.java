package com.patatrek.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita las tareas programadas (vencimiento de reservas pendientes de pago). */
@Configuration
@EnableScheduling
public class ProgramacionConfig {
}
