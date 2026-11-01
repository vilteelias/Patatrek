package com.patatrek.backend.devolucion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** CU-27 Registrar devolución de reserva, con los datos de ejemplo de DatosInicialesDevolucion. */
@SpringBootTest
@ActiveProfiles("demo")
@TestPropertySource(properties = {"patatrek.carpeta-uploads=target/test-uploads",
        "patatrek.revision-vencimientos-ms=600000",
        "patatrek.pasarela.permitir-forzar-resultado=true", "patatrek.correo.asincronico=false"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DevolucionApiTest {

    @Autowired WebApplicationContext contexto;

    MockMvc mvc;

    @BeforeEach
    void prepararMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private long idPendienteDe(String dni) throws Exception {
        String json = mvc.perform(get("/api/admin/devoluciones")).andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(json, "$.pendientes[?(@.dniCliente == '" + dni + "')].idReserva");
        return ids.get(0);
    }

    @Test
    void listaLasPendientesConMotivoYMontoAbonado() throws Exception {
        mvc.perform(get("/api/admin/devoluciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendientes", hasSize(3)))
                .andExpect(jsonPath("$.pendientes[*].estadoReserva", everyItem(is("PENDIENTE_DEVOLUCION"))))
                .andExpect(jsonPath("$.pendientes[*].motivo", everyItem(containsString("contingencia"))))
                .andExpect(jsonPath("$.realizadas", hasSize(1)))
                .andExpect(jsonPath("$.realizadas[0].nroTransaccionReintegro", startsWith("REI-")));
    }

    @Test
    void devuelveElTotalAbonadoYLaReservaQuedaDevuelta() throws Exception {
        // Julián Ferreyra pagó solo la seña (50 %): se le devuelve la seña
        long id = idPendienteDe("36444555");
        String json = mvc.perform(get("/api/admin/reservas/" + id)).andReturn().getResponse().getContentAsString();
        Number abonado = JsonPath.read(json, "$.importeAbonado");

        mvc.perform(post("/api/admin/devoluciones/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nroTransaccionReintegro", startsWith("REI-")))
                .andExpect(jsonPath("$.importeReintegrado", hasToString(matchesPattern(abonado.longValue() + "(\\.0+)?"))))
                .andExpect(jsonPath("$.devolucion.estadoReserva").value("DEVUELTA"))
                .andExpect(jsonPath("$.mensaje", containsString("Se reintegraron")));

        // La reserva queda Devuelta, el pago original Devuelto y aparece la transacción de reintegro
        mvc.perform(get("/api/admin/reservas/" + id))
                .andExpect(jsonPath("$.estadoReserva").value("DEVUELTA"))
                .andExpect(jsonPath("$.pagos", hasSize(2)))
                .andExpect(jsonPath("$.pagos[0].estadoPago").value("DEVUELTO"))
                .andExpect(jsonPath("$.pagos[1].modoPago").value("REINTEGRO"))
                .andExpect(jsonPath("$.pagos[1].estadoPago").value("APROBADO"));
        mvc.perform(get("/api/admin/reservas").param("dni", "36444555"))
                .andExpect(jsonPath("$[0].estadoPago").value("DEVUELTO"));

        // Deja de figurar como pendiente
        mvc.perform(get("/api/admin/devoluciones"))
                .andExpect(jsonPath("$.pendientes", hasSize(2)))
                .andExpect(jsonPath("$.realizadas", hasSize(2)));

        // No se puede devolver dos veces
        mvc.perform(post("/api/admin/devoluciones/" + id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("ya fue devuelta")));
    }

    @Test
    void soloSeDevuelvenReservasPendientesDeDevolucion() throws Exception {
        // Una reserva confirmada de los datos de ejemplo
        String json = mvc.perform(get("/api/admin/reservas").param("estado", "CONFIRMADA"))
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(json, "$[*].idReserva");

        mvc.perform(post("/api/admin/devoluciones/" + ids.get(0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("Pendiente de devolución")));
        mvc.perform(post("/api/admin/devoluciones/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void sinPendientesLaListaVieneVacia() throws Exception {
        for (String dni : List.of("36444555", "40222333", "32111999")) {
            mvc.perform(post("/api/admin/devoluciones/" + idPendienteDe(dni))).andExpect(status().isOk());
        }
        // Flujo 2a
        mvc.perform(get("/api/admin/devoluciones"))
                .andExpect(jsonPath("$.pendientes", hasSize(0)))
                .andExpect(jsonPath("$.montoTotalPendiente", hasToString(matchesPattern("0(\\.0+)?"))));
    }
}
