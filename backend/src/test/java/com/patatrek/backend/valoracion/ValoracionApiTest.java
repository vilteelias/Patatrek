package com.patatrek.backend.valoracion;

import com.jayway.jsonpath.JsonPath;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** CU-28 Registrar valoración y CU-29 Consultar valoraciones, con los datos de ejemplo. */
@SpringBootTest
@ActiveProfiles("demo")
@TestPropertySource(properties = {"patatrek.carpeta-uploads=target/test-uploads",
        "patatrek.revision-vencimientos-ms=600000"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ValoracionApiTest {

    @Autowired WebApplicationContext contexto;

    MockMvc mvc;

    @BeforeEach
    void prepararMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private long idReservaPendienteDeSofia() throws Exception {
        String json = mvc.perform(get("/api/cliente/valoraciones-pendientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].excursion").value("Sendero Laguna del Bosque"))
                .andReturn().getResponse().getContentAsString();
        return ((Integer) JsonPath.read(json, "$[0].idReserva")).longValue();
    }

    private long idReservaDe(String dniCliente) throws Exception {
        String json = mvc.perform(get("/api/admin/reservas").param("dni", dniCliente).param("estado", "CONFIRMADA"))
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(json, "$[*].idReserva");
        return ids.get(0);
    }

    @Test
    void elClienteValoraUnaSoloVez() throws Exception {
        long idReserva = idReservaPendienteDeSofia();

        mvc.perform(get("/api/valoraciones/formulario").param("reserva", String.valueOf(idReserva)).param("dni", "38555777"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acreditacion").value("Presente y Apto"))
                .andExpect(jsonPath("$.yaValorada").value(false));

        // 5a: sin calificación
        mvc.perform(post("/api/valoraciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idReserva\":" + idReserva + ",\"dni\":\"38555777\",\"comentario\":\"Hola\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.calificacion", containsString("entre 1 y 5 estrellas")));

        mvc.perform(post("/api/valoraciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idReserva\":" + idReserva + ",\"dni\":\"38555777\",\"calificacion\":5,\"comentario\":\"Hermoso\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.yaValorada").value(true))
                .andExpect(jsonPath("$.calificacion").value(5));

        // Una sola valoración por participante
        mvc.perform(post("/api/valoraciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idReserva\":" + idReserva + ",\"dni\":\"38555777\",\"calificacion\":3}"))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/cliente/valoraciones-pendientes")).andExpect(jsonPath("$", hasSize(0)));

        // Mateo (participante sin cuenta, con el enlace del correo) todavía puede valorar
        mvc.perform(get("/api/valoraciones/formulario").param("reserva", String.valueOf(idReserva)).param("dni", "45111222"))
                .andExpect(jsonPath("$.participante").value("Ibarra, Mateo"))
                .andExpect(jsonPath("$.yaValorada").value(false));
    }

    @Test
    void soloPresenteYAptoYDatosCorrectos() throws Exception {
        long idReserva = idReservaDe("33221144");

        // Nicolás Paz estuvo Ausente
        mvc.perform(get("/api/valoraciones/formulario").param("reserva", String.valueOf(idReserva)).param("dni", "34990011"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("Presente y Apto")));

        // DNI que no está en la reserva
        mvc.perform(get("/api/valoraciones/formulario").param("reserva", String.valueOf(idReserva)).param("dni", "11111111"))
                .andExpect(status().isNotFound());

        // Marcos ya valoró: el formulario muestra su valoración
        mvc.perform(get("/api/valoraciones/formulario").param("reserva", String.valueOf(idReserva)).param("dni", "33221144"))
                .andExpect(jsonPath("$.yaValorada").value(true))
                .andExpect(jsonPath("$.calificacion").value(5));
    }

    @Test
    void elAdministradorVePromedioYComentarios() throws Exception {
        String json = mvc.perform(get("/api/admin/valoraciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.excursion == 'Sendero Laguna del Bosque')].total", contains(2)))
                .andExpect(jsonPath("$[?(@.excursion == 'Sendero Laguna del Bosque')].promedio", contains(4.5)))
                .andExpect(jsonPath("$[?(@.excursion == 'Ascenso Cerro Perito Moreno')].total", contains(1)))
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(json, "$[?(@.excursion == 'Sendero Laguna del Bosque')].idExcursion");

        mvc.perform(get("/api/admin/valoraciones/excursiones/" + ids.get(0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadPorEstrellas", contains(0, 0, 0, 1, 1)))
                .andExpect(jsonPath("$.valoraciones", hasSize(2)))
                .andExpect(jsonPath("$.valoraciones[0].comentario", notNullValue()));
    }
}
