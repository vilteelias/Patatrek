package com.patatrek.backend.guia;

import com.jayway.jsonpath.JsonPath;
import com.patatrek.backend.salida.EstadoSalida;
import com.patatrek.backend.salida.SalidaRepository;
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
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Recorrido del guía (CU-10, CU-22, CU-15, CU-16) con los datos de ejemplo de DatosInicialesGuia.
 * El guía de demostración es Martín Almeida.
 */
@SpringBootTest
@ActiveProfiles("demo")
@TestPropertySource(properties = "patatrek.carpeta-uploads=target/test-uploads")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GuiaApiTest {

    @Autowired WebApplicationContext contexto;
    @Autowired SalidaRepository salidaRepository;

    MockMvc mvc;

    @BeforeEach
    void prepararMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    /** Id de la salida de una excursión en un estado dado. */
    private long idSalida(String nombreExcursion, EstadoSalida estado) {
        return salidaRepository.findIdsPorExcursionYEstado(nombreExcursion, estado).get(0);
    }

    @Test
    void elGuiaVeSoloSusSalidasConSuRol() throws Exception {
        mvc.perform(get("/api/guia/yo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apellido").value("Almeida"));

        mvc.perform(get("/api/guia/salidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].rolGuia", hasItem("AYUDANTE")))
                .andExpect(jsonPath("$[0].horaInicio", matchesPattern("\\d{2}:\\d{2}")));
    }

    @Test
    void nominaConCertificadosYEquipamientoSolicitado() throws Exception {
        long id = idSalida("Ascenso Cerro Perito Moreno", EstadoSalida.DISPONIBLE);

        mvc.perform(get("/api/guia/salidas/" + id))
                .andExpect(jsonPath("$.requiereCertificado").value(true))
                .andExpect(jsonPath("$.equipamientoObligatorio", hasSize(2)))
                .andExpect(jsonPath("$.guiasAyudantes[0]").value("Coronel, Brenda"));

        mvc.perform(get("/api/guia/salidas/" + id + "/participantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[?(@.apellido == 'Paredes')].certificado", contains(nullValue())))
                .andExpect(jsonPath("$[?(@.apellido == 'Molina')].certificado.estado", contains("CARGADO")));
    }

    @Test
    void controlIncompletoEsRechazadoYCompletoIniciaLaSalida() throws Exception {
        long id = idSalida("Ascenso Cerro Perito Moreno", EstadoSalida.DISPONIBLE);
        String participantes = mvc.perform(get("/api/guia/salidas/" + id + "/participantes"))
                .andReturn().getResponse().getContentAsString();
        String salida = mvc.perform(get("/api/guia/salidas/" + id)).andReturn().getResponse().getContentAsString();

        List<Number> idsParticipantes = JsonPath.read(participantes, "$[*].idParticipante");
        List<Number> idsEquipamiento = JsonPath.read(salida, "$.equipamientoObligatorio[*].idEquipamiento");
        String idsEquipo = idsEquipamiento.stream().map(n -> String.valueOf(n.longValue())).collect(Collectors.joining(","));

        // Solo el primero: faltan los demás (flujo 8a)
        long primero = idsParticipantes.get(0).longValue();
        mvc.perform(post("/api/guia/salidas/" + id + "/control-asistencia").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registros\":[{\"idParticipante\":" + primero
                                + ",\"estadoAsistencia\":\"PRESENTE\",\"equipamientoPresentado\":[" + idsEquipo + "]}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("Faltan registrar la asistencia de:")));

        // Todos presentes con todo el equipamiento
        StringBuilder registros = new StringBuilder();
        for (Number idParticipante : idsParticipantes) {
            if (registros.length() > 0) registros.append(',');
            registros.append("{\"idParticipante\":").append(idParticipante.longValue())
                    .append(",\"estadoAsistencia\":\"PRESENTE\",\"equipamientoPresentado\":[").append(idsEquipo).append("]}");
        }
        mvc.perform(post("/api/guia/salidas/" + id + "/control-asistencia").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registros\":[" + registros + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoSalida").value("EN_CURSO"));

        // Delgado y Vera tienen el certificado aprobado: Apto. Molina (cargado) y Paredes (sin certificado): No apto.
        mvc.perform(get("/api/guia/salidas/" + id + "/participantes"))
                .andExpect(jsonPath("$[?(@.estadoRequisitos == 'APTO')]", hasSize(2)))
                .andExpect(jsonPath("$[?(@.estadoRequisitos == 'NO_APTO')]", hasSize(2)));
    }

    @Test
    void elResponsableFinalizaYSeNotificaSoloAPresentesYAptos() throws Exception {
        long id = idSalida("Sendero Laguna del Bosque", EstadoSalida.EN_CURSO);

        mvc.perform(post("/api/guia/salidas/" + id + "/finalizacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observaciones\":\"  Sin incidentes.  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadNotificados").value(2))
                .andExpect(jsonPath("$.salida.estadoSalida").value("FINALIZADA"))
                .andExpect(jsonPath("$.salida.observaciones").value("Sin incidentes."));

        // No se puede volver a finalizar
        mvc.perform(post("/api/guia/salidas/" + id + "/finalizacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observaciones\":\"\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void elAyudanteNoPuedeFinalizar() throws Exception {
        long id = idSalida("Circuito Lagos Patagónicos", EstadoSalida.DISPONIBLE);
        mvc.perform(post("/api/guia/salidas/" + id + "/finalizacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observaciones\":\"\"}"))
                .andExpect(status().isForbidden());
    }
}
