package com.patatrek.backend.excursion;

import com.patatrek.backend.salida.EstadoSalida;
import com.patatrek.backend.salida.Salida;
import com.patatrek.backend.salida.SalidaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de los casos de uso CU-04 a CU-08 contra una base H2 en memoria
 * (perfil "demo", con los datos de ejemplo cargados).
 */
@SpringBootTest
@ActiveProfiles("demo")
@TestPropertySource(properties = "patatrek.carpeta-uploads=target/test-uploads")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ExcursionApiTest {

    @Autowired WebApplicationContext contexto;
    @Autowired ExcursionRepository excursionRepository;
    @Autowired SalidaRepository salidaRepository;

    MockMvc mvc;

    /** Se arma MockMvc a mano: funciona igual en Spring Boot 3 y 4. */
    @BeforeEach
    void prepararMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private static final String NUEVA = """
            {
              "nombre": "Glaciar Escondido",
              "descripcionGeneral": "Caminata hasta un glaciar colgante.",
              "descripcionTecnica": "Sendero de 14 km, desnivel 600 m.",
              "dificultad": "MEDIA",
              "duracion": 1,
              "precio": 70000,
              "recorridoBase": "Base – Glaciar – Base",
              "requiereCertificado": false,
              "aceptaMenores": true,
              "requiereEquipamiento": %s,
              "equipamientoIds": %s
            }""";

    @Test
    void catalogoPublicoSoloMuestraActivas() throws Exception {
        mvc.perform(get("/api/excursiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("Travesía Río Azul"))))
                .andExpect(jsonPath("$[0].motivoBaja").doesNotExist());
    }

    @Test
    void adminVeActivasEInactivas() throws Exception {
        mvc.perform(get("/api/admin/excursiones")).andExpect(jsonPath("$", hasSize(5)));
        mvc.perform(get("/api/admin/excursiones").param("estado", "INACTIVA"))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void registrarExcursionQuedaActiva() throws Exception {
        mvc.perform(post("/api/admin/excursiones").contentType(MediaType.APPLICATION_JSON)
                        .content(NUEVA.formatted("true", "[1, 2]")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoExcursion").value("ACTIVA"))
                .andExpect(jsonPath("$.equipamientoObligatorio", hasSize(2)));
    }

    @Test
    void registrarSinDatosObligatoriosDevuelveErroresPorCampo() throws Exception {
        mvc.perform(post("/api/admin/excursiones").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.dificultad").exists())
                .andExpect(jsonPath("$.errores.aceptaMenores").exists());
    }

    @Test
    void nombreDuplicadoAunqueEsteInactiva() throws Exception {
        String dup = NUEVA.formatted("false", "[]").replace("Glaciar Escondido", "travesía río azul");
        mvc.perform(post("/api/admin/excursiones").contentType(MediaType.APPLICATION_JSON).content(dup))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.nombre").exists());
    }

    @Test
    void equipamientoMarcadoSinItemsEsRechazado() throws Exception {
        mvc.perform(post("/api/admin/excursiones").contentType(MediaType.APPLICATION_JSON)
                        .content(NUEVA.formatted("true", "[]")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.equipamientoIds").exists());
    }

    @Test
    void conSalidasActivasSoloSePuedenEditarCamposInformativos() throws Exception {
        Excursion e = excursionRepository.findAllByOrderByNombreAsc().stream()
                .filter(x -> x.getNombre().startsWith("Sendero")).findFirst().orElseThrow();
        crearSalidaActiva(e);
        long id = e.getIdExcursion();

        String base = """
                {"nombre":"Sendero Laguna del Bosque (familias)","descripcionGeneral":"Nueva descripción",
                 "descripcionTecnica":"Sendero señalizado de 8 km con pendiente suave. Apto para principiantes.",
                 "dificultad":"BAJA","duracion":1,"precio":%s,
                 "recorridoBase":"Portada del parque – Bosque de lengas – Laguna del Bosque – regreso circular.",
                 "requiereCertificado":false,"aceptaMenores":true,"requiereEquipamiento":false,"equipamientoIds":[]}""";

        // Cambiar el precio está bloqueado
        mvc.perform(put("/api/admin/excursiones/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(base.formatted("50000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.precio").exists());

        // Cambiar nombre y descripción sí se permite
        mvc.perform(put("/api/admin/excursiones/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(base.formatted("42000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Sendero Laguna del Bosque (familias)"))
                .andExpect(jsonPath("$.tieneSalidasActivas").value(true));
    }

    @Test
    void bajaRequiereMotivoYSacaLaExcursionDelCatalogo() throws Exception {
        long id = excursionRepository.findByEstadoExcursionOrderByNombreAsc(EstadoExcursion.ACTIVA)
                .get(0).getIdExcursion();

        mvc.perform(patch("/api/admin/excursiones/" + id + "/baja").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").exists());

        mvc.perform(patch("/api/admin/excursiones/" + id + "/baja").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Temporada cerrada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoExcursion").value("INACTIVA"));

        mvc.perform(get("/api/excursiones/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/excursiones")).andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void imagenSoloAceptaPngOJpg() throws Exception {
        long id = excursionRepository.findAll().get(0).getIdExcursion();
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

        mvc.perform(multipart("/api/admin/excursiones/" + id + "/imagen")
                        .file(new MockMultipartFile("archivo", "foto.png", "image/png", png)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagenPromocional", startsWith("/uploads/excursiones/")));

        mvc.perform(multipart("/api/admin/excursiones/" + id + "/imagen")
                        .file(new MockMultipartFile("archivo", "virus.png", "image/png", "hola".getBytes())))
                .andExpect(status().isConflict());
    }

    private void crearSalidaActiva(Excursion e) {
        Salida s = new Salida();
        s.setExcursion(e);
        s.setFechaInicio(LocalDate.now().plusDays(20));
        s.setFechaFinalizacion(LocalDate.now().plusDays(20));
        s.setHoraInicio(LocalTime.of(8, 0));
        s.setHoraFinalizacion(LocalTime.of(17, 0));
        s.setCupoBase(10);
        s.setCupoMaximo(15);
        s.setCupoTotal(10);
        s.setEstadoSalida(EstadoSalida.DISPONIBLE);
        salidaRepository.save(s);
    }
}
