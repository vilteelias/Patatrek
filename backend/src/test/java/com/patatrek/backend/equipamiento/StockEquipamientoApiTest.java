package com.patatrek.backend.equipamiento;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Consulta de disponibilidad del equipamiento: el retenido y el prestado se ven en tiempo real. */
@SpringBootTest
@ActiveProfiles("demo")
@TestPropertySource(properties = {"patatrek.carpeta-uploads=target/test-uploads",
        "patatrek.revision-vencimientos-ms=600000"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class StockEquipamientoApiTest {

    @Autowired WebApplicationContext contexto;
    @Autowired ExcursionRepository excursionRepository;
    @Autowired EquipamientoRepository equipamientoRepository;

    MockMvc mvc;

    @BeforeEach
    void prepararMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private long idSalidaReservablePerito() throws Exception {
        long idExcursion = excursionRepository.findAllByOrderByNombreAsc().stream()
                .filter(e -> e.getNombre().equals("Ascenso Cerro Perito Moreno")).findFirst().orElseThrow()
                .getIdExcursion();
        String json = mvc.perform(get("/api/excursiones/" + idExcursion + "/salidas"))
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(json, "$[?(@.reservable == true)].idSalida");
        return ids.get(0);
    }

    @Test
    void sinSalidaMuestraElStockDeHoy() throws Exception {
        mvc.perform(get("/api/admin/equipamiento/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salida").doesNotExist())
                .andExpect(jsonPath("$.items.length()", greaterThan(0)))
                .andExpect(jsonPath("$.items[?(@.nombre == 'Casco')].stockTotal").exists())
                .andExpect(jsonPath("$.asignaciones", hasSize(0)));
        mvc.perform(get("/api/admin/equipamiento/salidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].estadoSalida", everyItem(is(oneOf("DISPONIBLE", "COMPLETA", "EN_CURSO")))));
    }

    @Test
    void unaReservaPendienteRetieneElEquipamiento() throws Exception {
        long idSalida = idSalidaReservablePerito();
        Equipamiento casco = equipamientoRepository.findAll().stream()
                .filter(e -> e.getNombre().equals("Casco")).findFirst().orElseThrow();
        String antes = mvc.perform(get("/api/admin/equipamiento/stock").param("idSalida", String.valueOf(idSalida)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salida.idSalida").value(idSalida))
                .andReturn().getResponse().getContentAsString();
        int disponibleAntes = JsonPath.<List<Integer>>read(antes, "$.items[?(@.nombre == 'Casco')].disponible").get(0);
        int retenidoAntes = JsonPath.<List<Integer>>read(antes, "$.items[?(@.nombre == 'Casco')].retenido").get(0);

        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idSalida":%d,"participantes":[{"dni":"30111221","nombre":"Ana","apellido":"Prueba",
                                 "correoElectronico":"ana@mail.com","nroTelefono":"11-2233-4455","fechaNacimiento":"1990-01-01"}],
                                 "equipamiento":[{"idEquipamiento":%d,"cantidad":1}]}""".formatted(idSalida, casco.getIdEquipamiento())))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/admin/equipamiento/stock").param("idSalida", String.valueOf(idSalida)))
                .andExpect(jsonPath("$.items[?(@.nombre == 'Casco')].retenido", contains(retenidoAntes + 1)))
                .andExpect(jsonPath("$.items[?(@.nombre == 'Casco')].disponible", contains(disponibleAntes - 1)))
                .andExpect(jsonPath("$.asignaciones[?(@.equipamiento == 'Casco')].estadoAsignacion", contains("RETENIDO")));
    }

    @Test
    void salidaInexistente() throws Exception {
        mvc.perform(get("/api/admin/equipamiento/stock").param("idSalida", "999999"))
                .andExpect(status().isNotFound());
    }
}
