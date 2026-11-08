package com.patatrek.backend.reserva;

import com.jayway.jsonpath.JsonPath;
import com.patatrek.backend.excursion.ExcursionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CU-19 del lado del participante (ID de reserva + DNI) y CU-26 4b: el participante paga el saldo restante. */
@SpringBootTest
@ActiveProfiles("demo")
@TestPropertySource(properties = {"patatrek.carpeta-uploads=target/test-uploads",
        "patatrek.revision-vencimientos-ms=600000",
        "patatrek.pasarela.permitir-forzar-resultado=true", "patatrek.correo.asincronico=false"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AccesoParticipanteApiTest {

    private static final String DNI_ACOMPANANTE = "30111223";

    @Autowired WebApplicationContext contexto;
    @Autowired ExcursionRepository excursionRepository;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    /** Reserva señada de la Laguna con la titular de demostración y un acompañante. */
    private long reservaSenada() throws Exception {
        long idExcursion = excursionRepository.findAllByOrderByNombreAsc().stream()
                .filter(e -> e.getNombre().equals("Sendero Laguna del Bosque")).findFirst().orElseThrow().getIdExcursion();
        String salidas = mvc.perform(get("/api/excursiones/" + idExcursion + "/salidas")).andReturn().getResponse().getContentAsString();
        int idSalida = JsonPath.<List<Integer>>read(salidas, "$[?(@.reservable == true)].idSalida").get(0);
        String json = mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"idSalida":%d,"participantes":[
                          {"dni":"38555777","nombre":"Sofía","apellido":"Ibarra","correoElectronico":"sofia.ibarra@mail.com",
                           "nroTelefono":"2804-667788","fechaNacimiento":"1994-03-22"},
                          {"dni":"%s","nombre":"Beto","apellido":"Prueba","correoElectronico":"beto@ejemplo.com",
                           "nroTelefono":"11-2233-4455","fechaNacimiento":"1990-01-01"}]}""".formatted(idSalida, DNI_ACOMPANANTE)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Integer) JsonPath.read(json, "$.idReserva")).longValue();
        mvc.perform(post("/api/reservas/" + id + "/pagos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"modoPago\":\"SENA\",\"aprobar\":true}")).andExpect(status().isOk());
        return id;
    }

    @Test
    void elParticipanteVeSuReservaYPuedePagarSoloElSaldo() throws Exception {
        long id = reservaSenada();
        mvc.perform(get("/api/participante/reservas/" + id).param("dni", DNI_ACOMPANANTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoReserva").value("SENADA"))
                .andExpect(jsonPath("$.participante.nombre").value("Beto"))
                .andExpect(jsonPath("$.modosPagoDisponibles", contains("SALDO")))
                .andExpect(jsonPath("$.participantes", hasSize(2)))
                // No se exponen los DNI ni los correos de los demás
                .andExpect(content -> org.assertj.core.api.Assertions
                        .assertThat(content.getResponse().getContentAsString()).doesNotContain("sofia.ibarra@mail.com"));

        mvc.perform(post("/api/participante/reservas/" + id + "/pagos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"" + DNI_ACOMPANANTE + "\",\"aprobar\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago").value("APROBADO"))
                .andExpect(jsonPath("$.reserva.estadoReserva").value("CONFIRMADA"))
                .andExpect(jsonPath("$.reserva.modosPagoDisponibles", hasSize(0)));

        // Ya está paga: no se puede volver a pagar
        mvc.perform(post("/api/participante/reservas/" + id + "/pagos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"" + DNI_ACOMPANANTE + "\",\"aprobar\":true}"))
                .andExpect(status().isConflict());
    }

    @Test
    void conDosIntentosFallidosSeBloqueaElAcceso() throws Exception {
        long id = reservaSenada();
        mvc.perform(get("/api/participante/reservas/" + id).param("dni", "11111111"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("no son correctos")));
        mvc.perform(get("/api/participante/reservas/" + id).param("dni", "22222222"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("5 minutos")));
        // Aunque ahora el DNI sea correcto, sigue bloqueado
        mvc.perform(get("/api/participante/reservas/" + id).param("dni", DNI_ACOMPANANTE))
                .andExpect(status().isConflict());
    }
}
