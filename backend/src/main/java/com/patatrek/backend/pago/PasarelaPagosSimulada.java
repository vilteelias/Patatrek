package com.patatrek.backend.pago;

import org.springframework.stereotype.Component;

import java.util.UUID;

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

    public Respuesta procesar(Pago pago, boolean aprobarSimulado) {
        String nroTransaccion = "SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        if (aprobarSimulado) {
            return new Respuesta(true, nroTransaccion, null);
        }
        return new Respuesta(false, nroTransaccion, "La entidad emisora rechazó el pago (fondos insuficientes).");
    }

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
