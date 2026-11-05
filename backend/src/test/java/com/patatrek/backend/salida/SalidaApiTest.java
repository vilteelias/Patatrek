package com.patatrek.backend.salida;

import com.jayway.jsonpath.JsonPath;
import com.patatrek.backend.excursion.Excursion;
import com.patatrek.backend.excursion.ExcursionRepository;
import com.patatrek.backend.soporte.Json;
import com.patatrek.backend.usuario.Guia;
import com.patatrek.backend.usuario.GuiaRepository;
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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CU-09 a CU-13: planificar, listar, asignar ayudantes y dar de baja salidas.
 * Usa las excursiones y los guías de los datos de ejemplo (perfil demo).
 */
@SpringBootTest
@ActiveProfiles("demo")
@TestPropertySource(properties = {"patatrek.carpeta-uploads=target/test-uploads",
        "patatrek.revision-vencimientos-ms=600000",
        "patatrek.pasarela.permitir-forzar-resultado=true", "patatrek.correo.asincronico=false"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SalidaApiTest {

    @Autowired WebApplicationContext contexto;
    @Autowired ExcursionRepository excursionRepository;
    @Autowired GuiaRepository guiaRepository;
    @Autowired RefugioRepository refugioRepository;
    @Autowired SalidaRepository salidaRepository;
    @Autowired TransactionTemplate tx;

    MockMvc mvc;

    @BeforeEach
    void prepararMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private long excursion(String nombre) {
        return excursionRepository.findAllByOrderByNombreAsc().stream()
                .filter(e -> e.getNombre().equals(nombre)).map(Excursion::getIdExcursion).findFirst().orElseThrow();
    }

    private long guia(String correo) {
        return guiaRepository.findByCorreoElectronicoIgnoreCase(correo).map(Guia::getIdUsuario).orElseThrow();
    }

    private long refugioCerroAlto() {
        return refugioRepository.findAll().stream().filter(r -> r.getNombre().equals("Refugio Cerro Alto"))
                .map(Refugio::getIdRefugio).findFirst().orElseThrow();
    }

    /** Salida de un día de la Laguna del Bosque dentro de {@code dias} días. */
    private Map<String, Object> laguna(int dias, String correoGuia, int cupoMaximo) {
        LocalDate dia = LocalDate.now().plusDays(dias);
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("idExcursion", excursion("Sendero Laguna del Bosque"));
        d.put("fechaInicio", dia.toString());
        d.put("horaInicio", "09:00");
        d.put("fechaFinalizacion", dia.toString());
        d.put("horaFinalizacion", "14:00");
        d.put("idGuiaResponsable", guia(correoGuia));
        d.put("cupoMaximo", cupoMaximo);
        return d;
    }

    private String cuerpo(Object o) {
        return Json.de(o);
    }

    /** Planifica la salida y devuelve la respuesta en JSON. */
    private String planificar(Map<String, Object> d) throws Exception {
        return mvc.perform(post("/api/admin/salidas").contentType(MediaType.APPLICATION_JSON).content(cuerpo(d)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static <T> T leer(String json, String ruta) {
        return JsonPath.read(json, ruta);
    }

    private static long id(String json) {
        return ((Number) JsonPath.read(json, "$.idSalida")).longValue();
    }

    @Test
    void planificaUnaSalidaDisponibleConElCupoBaseDelGuia() throws Exception {
        String s = planificar(laguna(60, "brenda.coronel@patatrek.com", 10));

        assertThat((String) leer(s, "$.estadoSalida")).isEqualTo("DISPONIBLE");
        assertThat((Integer) leer(s, "$.cupoBase")).isEqualTo(6);
        // El error de la rama original: la salida quedaba con cupo total 0 y en el catálogo figuraba "0 cupos"
        assertThat((Integer) leer(s, "$.cupoTotal")).isEqualTo(6);
        assertThat((Integer) leer(s, "$.cuposDisponibles")).isEqualTo(6);
        assertThat((String) leer(s, "$.guiaResponsable.nombre")).isEqualTo("Coronel, Brenda");
        assertThat((String) leer(s, "$.recorrido")).isNotBlank();

        long id = id(s);
        mvc.perform(get("/api/admin/salidas").param("excursion", "laguna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idSalida == " + id + ")].cupoTotal", contains(6)))
                .andExpect(jsonPath("$[*].excursion", everyItem(containsString("Laguna"))));

        // El cliente la ve en el catálogo con sus cupos
        mvc.perform(get("/api/excursiones/{id}/salidas", excursion("Sendero Laguna del Bosque")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idSalida == " + id + ")]", hasSize(1)));
    }

    @Test
    void validaLosDatosObligatoriosYLasReglas() throws Exception {
        mvc.perform(post("/api/admin/salidas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.idExcursion").exists())
                .andExpect(jsonPath("$.errores.fechaInicio").exists())
                .andExpect(jsonPath("$.errores.idGuiaResponsable").exists())
                .andExpect(jsonPath("$.errores.cupoMaximo").exists());

        // Fecha pasada
        Map<String, Object> pasada = laguna(-3, "brenda.coronel@patatrek.com", 10);
        mvc.perform(post("/api/admin/salidas").contentType(MediaType.APPLICATION_JSON).content(cuerpo(pasada)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fechaInicio", containsString("futuras")));

        // La Laguna dura un día: no puede terminar al día siguiente
        Map<String, Object> larga = laguna(60, "brenda.coronel@patatrek.com", 10);
        larga.put("fechaFinalizacion", LocalDate.now().plusDays(61).toString());
        mvc.perform(post("/api/admin/salidas").contentType(MediaType.APPLICATION_JSON).content(cuerpo(larga)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fechaFinalizacion", containsString("dura")));

        // Hora de fin antes que la de inicio
        Map<String, Object> horas = laguna(60, "brenda.coronel@patatrek.com", 10);
        horas.put("horaFinalizacion", "08:00");
        mvc.perform(post("/api/admin/salidas").contentType(MediaType.APPLICATION_JSON).content(cuerpo(horas)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.horaFinalizacion").exists());

        // El cupo máximo tiene que superar la capacidad del guía (6)
        Map<String, Object> cupo = laguna(60, "brenda.coronel@patatrek.com", 6);
        mvc.perform(post("/api/admin/salidas").contentType(MediaType.APPLICATION_JSON).content(cuerpo(cupo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cupoMaximo", containsString("mayor")));

        // Martín Almeida ya es responsable de la travesía de dentro de 20 días
        Map<String, Object> ocupado = laguna(21, "martin.almeida@patatrek.com", 12);
        mvc.perform(post("/api/admin/salidas").contentType(MediaType.APPLICATION_JSON).content(cuerpo(ocupado)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.idGuiaResponsable").exists());
    }

    @Test
    void unaSalidaDeVariosDiasExigeUnRefugioPorNoche() throws Exception {
        LocalDate inicio = LocalDate.now().plusDays(45);
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("idExcursion", excursion("Travesía Cerro Alto — Río Senguer"));
        d.put("fechaInicio", inicio.toString());
        d.put("horaInicio", "08:00");
        d.put("fechaFinalizacion", inicio.plusDays(2).toString());
        d.put("horaFinalizacion", "18:00");
        d.put("idGuiaResponsable", guia("lucas.ibarra@patatrek.com"));
        d.put("cupoMaximo", 12);

        mvc.perform(post("/api/admin/salidas").contentType(MediaType.APPLICATION_JSON).content(cuerpo(d)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.refugiosPorNoche").exists());

        // Más plazas que la capacidad del refugio (30)
        d.put("refugiosPorNoche", List.of(refugioCerroAlto(), refugioCerroAlto()));
        d.put("plazasPorRefugio", 40);
        mvc.perform(post("/api/admin/salidas").contentType(MediaType.APPLICATION_JSON).content(cuerpo(d)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.plazasPorRefugio").exists());

        d.put("plazasPorRefugio", 10);
        String s = planificar(d);
        assertThat((List<?>) leer(s, "$.refugios")).hasSize(1);
        assertThat((Integer) leer(s, "$.refugios[0].plazasReservadas")).isEqualTo(10);
        assertThat((Integer) leer(s, "$.cupoTotal")).isEqualTo(6);
    }

    @Test
    void losAyudantesRecalculanElCupoYRespetanElMaximo() throws Exception {
        long id = id(planificar(laguna(60, "lucas.ibarra@patatrek.com", 8)));
        long coronel = guia("brenda.coronel@patatrek.com");
        long almeida = guia("martin.almeida@patatrek.com");

        // 6 + floor(6 × 0,25 × 1) = 7
        mvc.perform(put("/api/admin/salidas/{id}/ayudantes", id).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("idsGuias", List.of(coronel)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cupoTotal").value(7))
                .andExpect(jsonPath("$.ayudantes", hasSize(1)));

        // 6 + floor(6 × 0,25 × 2) = 9 > 8 → 4b
        mvc.perform(put("/api/admin/salidas/{id}/ayudantes", id).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("idsGuias", List.of(coronel, almeida)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("cupo máximo")));

        // El responsable no puede ser ayudante
        mvc.perform(put("/api/admin/salidas/{id}/ayudantes", id).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("idsGuias", List.of(guia("lucas.ibarra@patatrek.com"))))))
                .andExpect(status().isConflict());

        // Quitar todos vuelve al cupo base
        mvc.perform(put("/api/admin/salidas/{id}/ayudantes", id).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("idsGuias", List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cupoTotal").value(6))
                .andExpect(jsonPath("$.ayudantes", hasSize(0)));
    }

    @Test
    void daDeBajaSoloSalidasSinReservasActivas() throws Exception {
        long id = id(planificar(laguna(60, "brenda.coronel@patatrek.com", 10)));

        mvc.perform(post("/api/admin/salidas/{id}/baja", id).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("motivo", " "))))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/admin/salidas/{id}/baja", id).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("motivo", "Se superpone con un evento en el parque."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoSalida").value("CANCELADA"))
                .andExpect(jsonPath("$.motivoCancelacion", containsString("Se superpone")));

        // La travesía de ejemplo tiene una reserva confirmada
        long conReservas = tx.execute(estado -> salidaRepository.findAll().stream()
                .filter(s -> s.getExcursion().getNombre().startsWith("Travesía Cerro Alto")
                        && s.getEstadoSalida() == EstadoSalida.DISPONIBLE)
                .map(Salida::getIdSalida).findFirst().orElseThrow());
        mvc.perform(post("/api/admin/salidas/{id}/baja", conReservas).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("motivo", "Prueba"))))
                .andExpect(status().isConflict());
    }
}
