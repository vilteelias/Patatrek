package com.patatrek.backend.config;

import com.jayway.jsonpath.JsonPath;
import com.patatrek.backend.reserva.EstadoReserva;
import com.patatrek.backend.reserva.Participante;
import com.patatrek.backend.reserva.ParticipanteRepository;
import com.patatrek.backend.reserva.ReservaRepository;
import com.patatrek.backend.salida.EstadoSalida;
import com.patatrek.backend.salida.SalidaRepository;
import com.patatrek.backend.usuario.ClienteRepository;
import com.patatrek.backend.usuario.GuiaRepository;
import com.patatrek.backend.valoracion.ValoracionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Carga los datos de la exposición (DatosExposicion) en la base en memoria y revisa que sean coherentes y que las
 * pantallas principales los puedan mostrar. DatosExposicion ya verifica cupos, plazas y guías al cargar.
 */
@SpringBootTest
@ActiveProfiles("demo")
@TestPropertySource(properties = {"patatrek.carpeta-uploads=target/test-uploads",
        "patatrek.archivos.directorio=target/test-uploads",
        "patatrek.revision-vencimientos-ms=600000",
        "patatrek.cargar-datos-demo=false", "patatrek.cargar-datos-exposicion=true",
        "patatrek.pasarela.permitir-forzar-resultado=true", "patatrek.correo.asincronico=false"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DatosExposicionTest {

    @Autowired WebApplicationContext contexto;
    @Autowired SalidaRepository salidaRepository;
    @Autowired ReservaRepository reservaRepository;
    @Autowired ParticipanteRepository participanteRepository;
    @Autowired ClienteRepository clienteRepository;
    @Autowired GuiaRepository guiaRepository;
    @Autowired ValoracionRepository valoracionRepository;
    @Autowired TransactionTemplate tx;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void cargaUnaBuenaCantidadDeDatosEnTodosLosEstados() {
        assertThat(guiaRepository.count()).isGreaterThanOrEqualTo(7);
        assertThat(clienteRepository.count()).isGreaterThanOrEqualTo(25);
        assertThat(reservaRepository.count()).isGreaterThanOrEqualTo(50);
        assertThat(participanteRepository.count()).isGreaterThanOrEqualTo(90);
        assertThat(valoracionRepository.count()).isGreaterThanOrEqualTo(15);
        assertThat(salidaRepository.findAll()).extracting(s -> s.getEstadoSalida())
                .contains(EstadoSalida.DISPONIBLE, EstadoSalida.COMPLETA, EstadoSalida.EN_CURSO,
                        EstadoSalida.FINALIZADA, EstadoSalida.CANCELADA);
        assertThat(reservaRepository.countByEstadoReservaIn(EnumSet.of(EstadoReserva.PENDIENTE_DEVOLUCION))).isEqualTo(3);
        assertThat(reservaRepository.countByEstadoReservaIn(EnumSet.of(EstadoReserva.DEVUELTA))).isEqualTo(3);
    }

    @Test
    void lasPantallasDelAdministradorYDelCatalogoLosMuestran() throws Exception {
        mvc.perform(get("/api/admin/salidas")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(25))))
                .andExpect(jsonPath("$[?(@.estadoSalida == 'FINALIZADA')].observaciones", hasItem(notNullValue())));
        mvc.perform(get("/api/excursiones")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));
        mvc.perform(get("/api/admin/reservas")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(50))));
    }

    @Test
    void laCancelacionAutomaticaCancelaLasDosReservasPreparadas() throws Exception {
        String json = mvc.perform(post("/api/admin/reservas/cancelacion-automatica"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Object> canceladas = JsonPath.read(json, "$..idReserva");
        assertThat(canceladas).hasSize(2);
    }

    @Test
    void elAcompananteDeSofiaPuedeEntrarConSuDniYPagarElSaldo() throws Exception {
        long[] reservaYDni = tx.execute(estado -> {
            Participante mateo = participanteRepository.findAll().stream()
                    .filter(p -> p.getNombre().equals("Mateo") && p.getApellido().equals("Ibarra")
                            && p.getReserva().getEstadoReserva() == EstadoReserva.SENADA)
                    .findFirst().orElseThrow();
            return new long[]{mateo.getReserva().getIdReserva(), Long.parseLong(mateo.getDni())};
        });
        mvc.perform(get("/api/participante/reservas/" + reservaYDni[0]).param("dni", String.valueOf(reservaYDni[1])))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modosPagoDisponibles", contains("SALDO")));
    }
}
