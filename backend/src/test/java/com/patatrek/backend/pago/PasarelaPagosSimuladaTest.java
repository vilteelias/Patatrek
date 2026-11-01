package com.patatrek.backend.pago;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** La pasarela decide sola: el resultado que manda la pantalla solo se respeta en las pruebas. */
class PasarelaPagosSimuladaTest {

    @Test
    void ignoraElResultadoPedidoSiNoEstaPermitido() {
        PasarelaPagosSimulada siempreAprueba = new PasarelaPagosSimulada(1.0, false);
        PasarelaPagosSimulada siempreRechaza = new PasarelaPagosSimulada(0.0, false);
        assertThat(siempreAprueba.procesar(null, false).aprobado()).isTrue();
        assertThat(siempreRechaza.procesar(null, true).aprobado()).isFalse();
        assertThat(siempreRechaza.procesar(null, null).motivoRechazo()).isNotBlank();
    }

    @Test
    void enLasPruebasSePuedeForzar() {
        PasarelaPagosSimulada p = new PasarelaPagosSimulada(0.5, true);
        assertThat(p.procesar(null, true).aprobado()).isTrue();
        assertThat(p.procesar(null, false).aprobado()).isFalse();
    }

    @Test
    void alAzarDaAmbosResultados() {
        PasarelaPagosSimulada p = new PasarelaPagosSimulada(0.5, false);
        long aprobados = IntStream.range(0, 400).filter(i -> p.procesar(null, null).aprobado()).count();
        assertThat(aprobados).isBetween(120L, 280L);
        assertThat(p.procesar(null, null).nroTransaccion()).startsWith("SIM-");
    }
}
