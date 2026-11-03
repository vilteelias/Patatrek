package com.patatrek.backend.salida;

import com.jayway.jsonpath.JsonPath;
import com.patatrek.backend.excursion.Excursion;
import com.patatrek.backend.excursion.ExcursionRepository;
import com.patatrek.backend.notificacion.RegistroCorreos;
import com.patatrek.backend.reserva.EstadoReserva;
import com.patatrek.backend.reserva.Participante;
import com.patatrek.backend.reserva.ParticipanteRepository;
import com.patatrek.backend.reserva.Reserva;
import com.patatrek.backend.reserva.ReservaRepository;
import com.patatrek.backend.soporte.Json;
import com.patatrek.backend.usuario.Cliente;
import com.patatrek.backend.usuario.ClienteRepository;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CU-14 Gestionar contingencias y la respuesta del cliente a una reprogramación (RF-12).
 * Cada prueba planifica una salida nueva de la Laguna del Bosque y le agrega una reserva confirmada
 * del cliente de demostración (sofia.ibarra@mail.com) con un acompañante.
 */
@SpringBootTest
@ActiveProfiles("demo")
@TestPropertySource(properties = {"patatrek.carpeta-uploads=target/test-uploads",
        "patatrek.revision-vencimientos-ms=600000",
        "patatrek.pasarela.permitir-forzar-resultado=true", "patatrek.correo.asincronico=false"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ContingenciaApiTest {

    private static final String TITULAR = "sofia.ibarra@mail.com";
    private static final String ACOMPANANTE = "acompanante.contingencia@ejemplo.com";

    @Autowired WebApplicationContext contexto;
    @Autowired TransactionTemplate tx;
    @Autowired ExcursionRepository excursionRepository;
    @Autowired GuiaRepository guiaRepository;
    @Autowired SalidaRepository salidaRepository;
    @Autowired ReservaRepository reservaRepository;
    @Autowired ParticipanteRepository participanteRepository;
    @Autowired ClienteRepository clienteRepository;
    @Autowired RegistroCorreos registroCorreos;

    MockMvc mvc;
    long idSalida;
    long idReserva;

    @BeforeEach
    void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();

        LocalDate dia = LocalDate.now().plusDays(60);
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("idExcursion", excursionRepository.findAllByOrderByNombreAsc().stream()
                .filter(e -> e.getNombre().equals("Sendero Laguna del Bosque"))
                .map(Excursion::getIdExcursion).findFirst().orElseThrow());
        d.put("fechaInicio", dia.toString());
        d.put("horaInicio", "09:00");
        d.put("fechaFinalizacion", dia.toString());
        d.put("horaFinalizacion", "14:00");
        d.put("idGuiaResponsable", guiaRepository.findByCorreoElectronicoIgnoreCase("brenda.coronel@patatrek.com")
                .map(Guia::getIdUsuario).orElseThrow());
        d.put("cupoMaximo", 10);
        String s = mvc.perform(post("/api/admin/salidas").contentType(MediaType.APPLICATION_JSON).content(Json.de(d)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        idSalida = ((Number) JsonPath.read(s, "$.idSalida")).longValue();

        idReserva = tx.execute(estado -> {
            Salida salida = salidaRepository.findById(idSalida).orElseThrow();
            Cliente titular = clienteRepository.findByCorreoElectronicoIgnoreCase(TITULAR).orElseThrow();
            Reserva r = new Reserva();
            r.setSalida(salida);
            r.setCliente(titular);
            r.setFechaHora(LocalDateTime.now().minusDays(1));
            r.setEstadoReserva(EstadoReserva.CONFIRMADA);
            r.setCantidadParticipantes(2);
            r.setMontoTotal(new BigDecimal("84000"));
            r.setSaldoPendiente(BigDecimal.ZERO);
            r = reservaRepository.save(r);
            participanteRepository.save(new Participante(r, titular.getDni(), titular.getNombre(), titular.getApellido(),
                    TITULAR, "11-2233-4455", LocalDate.of(1990, 1, 1)));
            participanteRepository.save(new Participante(r, "44123456", "Nico", "Prueba", ACOMPANANTE,
                    "11-2233-4455", LocalDate.of(1995, 5, 5)));
            salida.setCuposOcupados(salida.getCuposOcupados() + 2);
            salidaRepository.save(salida);
            return r.getIdReserva();
        });
        registroCorreos.limpiar();
    }

    private org.springframework.test.web.servlet.ResultActions contingencia(Map<String, Object> d) throws Exception {
        return mvc.perform(post("/api/admin/salidas/{id}/contingencias", idSalida)
                .contentType(MediaType.APPLICATION_JSON).content(Json.de(d)));
    }

    @Test
    void validaLosDatosObligatorios() throws Exception {
        contingencia(Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tipoContingencia").exists())
                .andExpect(jsonPath("$.errores.resolucion").exists())
                .andExpect(jsonPath("$.errores.motivo").exists());

        contingencia(Map.of("tipoContingencia", "OPERATIVA", "resolucion", "MODIFICACION", "motivo", "Derrumbe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nuevoRecorrido").exists());

        contingencia(Map.of("tipoContingencia", "CLIMATICA", "resolucion", "REPROGRAMACION", "motivo", "Nevada"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nuevaFechaInicio").exists());
    }

    @Test
    void laCancelacionPasaLasReservasAPendienteDeDevolucionYAvisa() throws Exception {
        contingencia(Map.of("tipoContingencia", "CLIMATICA", "resolucion", "CANCELACION",
                "motivo", "Alerta por vientos fuertes."))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservasAfectadas").value(1))
                .andExpect(jsonPath("$.salida.estadoSalida").value("CANCELADA"));

        Reserva r = reservaRepository.findById(idReserva).orElseThrow();
        assertThat(r.getEstadoReserva()).isEqualTo(EstadoReserva.PENDIENTE_DEVOLUCION);
        assertThat(r.getMotivoCancelacion()).contains("climática").contains("vientos fuertes");
        assertThat(salidaRepository.findById(idSalida).orElseThrow().getCuposOcupados()).isZero();
        // Se avisa al titular y a cada participante con correo propio
        assertThat(registroCorreos.para(TITULAR)).singleElement()
                .satisfies(e -> assertThat(e.correo().asunto()).contains("Se canceló"));
        assertThat(registroCorreos.para(ACOMPANANTE)).hasSize(1);

        // Ya cancelada no admite otra contingencia
        contingencia(Map.of("tipoContingencia", "CLIMATICA", "resolucion", "CANCELACION", "motivo", "Otra"))
                .andExpect(status().isConflict());
    }

    @Test
    void laModificacionCambiaElRecorrido() throws Exception {
        contingencia(Map.of("tipoContingencia", "OPERATIVA", "resolucion", "MODIFICACION",
                "motivo", "Cierre del puente norte.", "nuevoRecorrido", "Acceso por el sendero sur."))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.salida.recorrido").value("Acceso por el sendero sur."))
                .andExpect(jsonPath("$.salida.estadoSalida").value("DISPONIBLE"))
                .andExpect(jsonPath("$.salida.contingencias", hasSize(1)));
        assertThat(reservaRepository.findById(idReserva).orElseThrow().getEstadoReserva())
                .isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(registroCorreos.para(TITULAR)).hasSize(1);
    }

    private void reprogramar() throws Exception {
        LocalDate nueva = LocalDate.now().plusDays(62);
        contingencia(Map.of("tipoContingencia", "CLIMATICA", "resolucion", "REPROGRAMACION",
                "motivo", "Pronóstico de nevadas.", "nuevaFechaInicio", nueva.toString(), "nuevaHoraInicio", "10:00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.salida.fechaInicio").value(nueva.toString()));
        assertThat(reservaRepository.findById(idReserva).orElseThrow().isReprogramacionPendiente()).isTrue();
        assertThat(registroCorreos.para(TITULAR)).hasSize(1);

        mvc.perform(get("/api/reservas/{id}", idReserva))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reprogramacion.motivo").value("Pronóstico de nevadas."))
                .andExpect(jsonPath("$.reprogramacion.fechaAnterior", startsWith(LocalDate.now().plusDays(60).toString())));
        mvc.perform(get("/api/cliente/reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idReserva == " + idReserva + ")].reprogramacionPendiente", contains(true)));
    }

    @Test
    void elClienteAceptaLaReprogramacion() throws Exception {
        reprogramar();
        mvc.perform(post("/api/reservas/{id}/reprogramacion", idReserva)
                        .contentType(MediaType.APPLICATION_JSON).content(Json.de(Map.of("acepta", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoReserva").value("CONFIRMADA"))
                .andExpect(jsonPath("$.reprogramacion").doesNotExist());

        // No se puede responder dos veces
        mvc.perform(post("/api/reservas/{id}/reprogramacion", idReserva)
                        .contentType(MediaType.APPLICATION_JSON).content(Json.de(Map.of("acepta", false))))
                .andExpect(status().isConflict());
    }

    @Test
    void elClienteRechazaLaReprogramacionYQuedaParaDevolucion() throws Exception {
        reprogramar();
        mvc.perform(post("/api/reservas/{id}/reprogramacion", idReserva)
                        .contentType(MediaType.APPLICATION_JSON).content(Json.de(Map.of("acepta", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoReserva").value("PENDIENTE_DEVOLUCION"));
        assertThat(salidaRepository.findById(idSalida).orElseThrow().getCuposOcupados()).isZero();
    }
}
