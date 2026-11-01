package com.patatrek.backend.pago;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Actor externo "Pasarela de Pagos" (simulada).
 *
 * En un sistema real, el cliente ingresa los datos de su tarjeta en la página de la pasarela
 * y PataTrek solo recibe la respuesta. Por eso este componente no recibe ningún dato de tarjeta:
 * solo el importe y el identificador de la operación, y devuelve aprobado/rechazado con un número de transacción.
 */
@Component
public class PasarelaPagosSimulada {

    public record Respuesta(boolean aprobado, String nroTransaccion, String motivoRechazo) {}

    private final double probabilidadAprobacion;
    private final boolean permitirForzarResultado;

    public PasarelaPagosSimulada(
            @Value("${patatrek.pasarela.probabilidad-aprobacion:0.8}") double probabilidadAprobacion,
            @Value("${patatrek.pasarela.permitir-forzar-resultado:false}") boolean permitirForzarResultado) {
        this.probabilidadAprobacion = Math.max(0, Math.min(1, probabilidadAprobacion));
        this.permitirForzarResultado = permitirForzarResultado;
    }

    /**
     * Procesa el cobro. Como una pasarela real, la pasarela decide sola si lo aprueba o lo rechaza:
     * al azar, con la probabilidad de aprobación configurada (por defecto 80 %).
     * resultadoForzado solo se respeta en las pruebas automáticas (patatrek.pasarela.permitir-forzar-resultado=true).
     */
    public Respuesta procesar(Pago pago, Boolean resultadoForzado) {
        String nroTransaccion = "SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        boolean aprobado = permitirForzarResultado && resultadoForzado != null
                ? resultadoForzado
                : ThreadLocalRandom.current().nextDouble() < probabilidadAprobacion;
        if (aprobado) {
            return new Respuesta(true, nroTransaccion, null);
        }
        return new Respuesta(false, nroTransaccion, MOTIVOS_RECHAZO[ThreadLocalRandom.current().nextInt(MOTIVOS_RECHAZO.length)]);
    }

    private static final String[] MOTIVOS_RECHAZO = {
            "La entidad emisora rechazó el pago (fondos insuficientes).",
            "La entidad emisora rechazó el pago (el medio de pago no está habilitado para compras en línea).",
            "La pasarela no pudo validar el pago con la entidad emisora."
    };

    /**
     * CU-27 pasos 5-6: solicitud de reembolso. PataTrek envía el identificador de la transacción original
     * y el monto a reintegrar; la pasarela lo devuelve al mismo medio con el que pagó el cliente
     * (PataTrek tampoco conoce ese medio) y responde con el número de transacción de reintegro.
     */
    public Respuesta reintegrar(Pago reintegro, String nroTransaccionOriginal) {
        String nroTransaccion = "REI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new Respuesta(true, nroTransaccion, null);
    }
}
