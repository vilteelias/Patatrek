package com.patatrek.backend.reserva;

import com.jayway.jsonpath.JsonPath;
import com.patatrek.backend.equipamiento.Equipamiento;
import com.patatrek.backend.equipamiento.EquipamientoRepository;
import com.patatrek.backend.excursion.ExcursionRepository;
import com.patatrek.backend.salida.Salida;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** CU-18 Realizar reserva y CU-26 Realizar pago, con los datos de ejemplo. */
@SpringBootTest
@ActiveProfiles("demo")
@TestPropertySource(properties = {"patatrek.carpeta-uploads=target/test-uploads",
        "patatrek.revision-vencimientos-ms=600000"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReservaApiTest {

    @Autowired WebApplicationContext contexto;
    @Autowired ExcursionRepository excursionRepository;
    @Autowired SalidaRepository salidaRepository;
    @Autowired EquipamientoRepository equipamientoRepository;

    MockMvc mvc;

    @BeforeEach
    void prepararMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    /** Los importes son BigDecimal: en el JSON pueden venir como 95000 o 95000.00. */
    private static org.hamcrest.Matcher<Object> importe(long valor) {
        return hasToString(matchesPattern(valor + "(\\.0+)?"));
    }

    private long idExcursion(String nombre) {
        return excursionRepository.findAllByOrderByNombreAsc().stream()
                .filter(e -> e.getNombre().equals(nombre)).findFirst().orElseThrow().getIdExcursion();
    }

    /** Primera salida reservable de la excursión. */
    private long idSalidaReservable(String excursion) throws Exception {
        String json = mvc.perform(get("/api/excursiones/" + idExcursion(excursion) + "/salidas"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(json, "$[?(@.reservable == true)].idSalida");
        return ids.get(0);
    }

    private String participante(String dni, String nombre, String nacimiento) {
        return """
                {"dni":"%s","nombre":"%s","apellido":"Prueba","correoElectronico":"%s@mail.com",
                 "nroTelefono":"11-2233-4455","fechaNacimiento":"%s"}""".formatted(dni, nombre, nombre.toLowerCase(), nacimiento);
    }

    private long crearReservaLaguna(int participantes) throws Exception {
        long idSalida = idSalidaReservable("Sendero Laguna del Bosque");
        StringBuilder lista = new StringBuilder();
        for (int i = 0; i < participantes; i++) {
            if (i > 0) lista.append(',');
            lista.append(participante("3000000" + i, "Persona" + i, "1990-05-10"));
        }
        String json = mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idSalida\":" + idSalida + ",\"participantes\":[" + lista + "]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoReserva").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.segundosParaVencer", greaterThan(500)))
                .andReturn().getResponse().getContentAsString();
        return ((Integer) JsonPath.read(json, "$.idReserva")).longValue();
    }

    @Test
    void listaSalidasConMotivoCuandoNoSePuedeReservar() throws Exception {
        mvc.perform(get("/api/cliente/yo")).andExpect(jsonPath("$.apellido").value("Ibarra"));

        mvc.perform(get("/api/excursiones/" + idExcursion("Ascenso Cerro Perito Moreno") + "/salidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.reservable == false)].motivoNoReservable",
                        hasItem(containsString("72 horas"))))
                .andExpect(jsonPath("$[?(@.reservable == true)].cuposDisponibles", hasItem(2)));
    }

    @Test
    void camposObligatoriosDelParticipanteSeMarcanUnoPorUno() throws Exception {
        long idSalida = idSalidaReservable("Sendero Laguna del Bosque");
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idSalida\":" + idSalida + ",\"participantes\":["
                                + participante("30111222", "Ana", "1990-01-01") + ","
                                + "{\"dni\":\"30111222\",\"nombre\":\"\",\"apellido\":\"X\",\"correoElectronico\":\"mal\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['participantes[1].dni']", containsString("otro participante")))
                .andExpect(jsonPath("$.errores['participantes[1].nombre']").value("El nombre es obligatorio."))
                .andExpect(jsonPath("$.errores['participantes[1].correoElectronico']", containsString("válido")))
                .andExpect(jsonPath("$.errores['participantes[1].nroTelefono']").exists())
                .andExpect(jsonPath("$.errores['participantes[1].fechaNacimiento']").exists())
                .andExpect(jsonPath("$.errores['participantes[0].dni']").doesNotExist());
    }

    @Test
    void rechazaMenoresYMasParticipantesQueCupos() throws Exception {
        long idSalida = idSalidaReservable("Ascenso Cerro Perito Moreno");
        String menor = LocalDate.now().minusYears(15).toString();

        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idSalida\":" + idSalida + ",\"participantes\":["
                                + participante("30111222", "Ana", menor) + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['participantes[0].fechaNacimiento']", containsString("menores")));

        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idSalida\":" + idSalida + ",\"participantes\":["
                                + participante("30111221", "Ana", "1990-01-01") + ","
                                + participante("30111222", "Beto", "1990-01-01") + ","
                                + participante("30111223", "Caro", "1990-01-01") + "]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("quedan 2")));
    }

    @Test
    void retieneRecursosYConsolidaAlAprobarLaSena() throws Exception {
        long idSalida = idSalidaReservable("Ascenso Cerro Perito Moreno");
        Equipamiento casco = equipamientoRepository.findAll().stream()
                .filter(e -> e.getNombre().equals("Casco")).findFirst().orElseThrow();

        String json = mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idSalida\":" + idSalida + ",\"participantes\":["
                                + participante("30111221", "Ana", "1990-01-01") + ","
                                + participante("30111222", "Beto", "1991-01-01") + "],"
                                + "\"equipamiento\":[{\"idEquipamiento\":" + casco.getIdEquipamiento() + ",\"cantidad\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montoTotal", importe(190000)))
                .andExpect(jsonPath("$.importeSena", importe(95000)))
                .andExpect(jsonPath("$.modosPagoDisponibles", contains("SENA", "TOTAL")))
                .andExpect(jsonPath("$.equipamiento[0].estado").value("RETENIDO"))
                .andReturn().getResponse().getContentAsString();
        long idReserva = ((Integer) JsonPath.read(json, "$.idReserva")).longValue();

        Salida retenida = salidaRepository.findById(idSalida).orElseThrow();
        assertThat(retenida.getCuposRetenidos()).isEqualTo(2);
        assertThat(retenida.getCuposDisponibles()).isZero();
        assertThat(equipamientoRepository.findById(casco.getIdEquipamiento()).orElseThrow().getStockRetenido()).isEqualTo(2);

        mvc.perform(post("/api/reservas/" + idReserva + "/pagos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modoPago\":\"SENA\",\"aprobar\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago").value("APROBADO"))
                .andExpect(jsonPath("$.nroTransaccion", startsWith("SIM-")))
                .andExpect(jsonPath("$.reserva.estadoReserva").value("SENADA"))
                .andExpect(jsonPath("$.reserva.saldoPendiente", importe(95000)))
                .andExpect(jsonPath("$.reserva.equipamiento[0].estado").value("CONSOLIDADO"))
                .andExpect(jsonPath("$.reserva.modosPagoDisponibles", contains("SALDO")));

        Salida consolidada = salidaRepository.findById(idSalida).orElseThrow();
        assertThat(consolidada.getCuposRetenidos()).isZero();
        assertThat(consolidada.getCuposOcupados()).isEqualTo(14);
        assertThat(consolidada.getEstadoSalida().name()).isEqualTo("COMPLETA");
        assertThat(equipamientoRepository.findById(casco.getIdEquipamiento()).orElseThrow().getStockRetenido()).isZero();

        mvc.perform(post("/api/reservas/" + idReserva + "/pagos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modoPago\":\"SALDO\",\"aprobar\":true}"))
                .andExpect(jsonPath("$.reserva.estadoReserva").value("CONFIRMADA"))
                .andExpect(jsonPath("$.reserva.saldoPendiente", importe(0)))
                .andExpect(jsonPath("$.reserva.pagos", hasSize(2)));
    }

    @Test
    void pagoRechazadoCancelaYLiberaLoRetenido() throws Exception {
        long idReserva = crearReservaLaguna(2);

        mvc.perform(post("/api/reservas/" + idReserva + "/pagos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modoPago\":\"TOTAL\",\"aprobar\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago").value("RECHAZADO"))
                .andExpect(jsonPath("$.reserva.estadoReserva").value("CANCELADA"))
                .andExpect(jsonPath("$.reserva.pagos[0].estadoPago").value("RECHAZADO"));

        long idSalida = idSalidaReservable("Sendero Laguna del Bosque");
        assertThat(salidaRepository.findById(idSalida).orElseThrow().getCuposRetenidos()).isZero();
    }

    @Test
    void alVencerLaRetencionSeCancelaYNoSePuedePagar() throws Exception {
        long idReserva = crearReservaLaguna(1);

        mvc.perform(post("/api/reservas/" + idReserva + "/simular-vencimiento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoReserva").value("CANCELADA"))
                .andExpect(jsonPath("$.motivoCancelacion", containsString("Venció")))
                .andExpect(jsonPath("$.modosPagoDisponibles", hasSize(0)));

        mvc.perform(post("/api/reservas/" + idReserva + "/pagos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modoPago\":\"TOTAL\",\"aprobar\":true}"))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/cliente/reservas"))
                .andExpect(jsonPath("$[0].estadoReserva").value("CANCELADA"));
    }

    @Test
    void cancelarReservaPagadaLiberaCuposSinDevolucion() throws Exception {
        long idReserva = crearReservaLaguna(2);
        long idSalida = idSalidaReservable("Sendero Laguna del Bosque");
        int ocupadosAntes = salidaRepository.findById(idSalida).orElseThrow().getCuposOcupados();

        mvc.perform(post("/api/reservas/" + idReserva + "/pagos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modoPago\":\"SENA\",\"aprobar\":true}"))
                .andExpect(jsonPath("$.reserva.cancelable").value(true));
        assertThat(salidaRepository.findById(idSalida).orElseThrow().getCuposOcupados()).isEqualTo(ocupadosAntes + 2);

        mvc.perform(post("/api/reservas/" + idReserva + "/cancelacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Cambio de planes\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoReserva").value("CANCELADA"))
                .andExpect(jsonPath("$.motivoCancelacion", containsString("Cambio de planes")))
                .andExpect(jsonPath("$.importeAbonado", importe(42000)))
                .andExpect(jsonPath("$.cancelable").value(false));

        assertThat(salidaRepository.findById(idSalida).orElseThrow().getCuposOcupados()).isEqualTo(ocupadosAntes);

        // No se puede cancelar dos veces
        mvc.perform(post("/api/reservas/" + idReserva + "/cancelacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void reservaPendienteDePagoNoSeCancelaManualmente() throws Exception {
        long idReserva = crearReservaLaguna(1);
        mvc.perform(post("/api/reservas/" + idReserva + "/cancelacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("Señada o Confirmada")));
    }

    @Test
    void misReservasYReservasGeneralesConFiltros() throws Exception {
        long idReserva = crearReservaLaguna(1);

        mvc.perform(get("/api/cliente/reservas").param("estado", "PENDIENTE_PAGO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idReserva").value(idReserva))
                .andExpect(jsonPath("$[0].estadoPago").value("PENDIENTE"));

        mvc.perform(get("/api/cliente/reservas").param("estado", "CONFIRMADA"))
                .andExpect(jsonPath("$", hasSize(0)));

        // Administrador: todas las reservas (las de ejemplo + la nueva), filtrando por DNI y por excursión
        mvc.perform(get("/api/admin/reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(5)));
        mvc.perform(get("/api/admin/reservas").param("dni", "38555777"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cliente").value("Ibarra, Sofía"));
        mvc.perform(get("/api/admin/reservas").param("excursion", "perito").param("estado", "CONFIRMADA"))
                .andExpect(jsonPath("$[*].excursion", everyItem(is("Ascenso Cerro Perito Moreno"))))
                .andExpect(jsonPath("$[*].estadoPago", everyItem(is("APROBADO"))));

        // Rango de fechas invertido
        mvc.perform(get("/api/admin/reservas").param("desde", "2026-12-01").param("hasta", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.hasta").exists());
        // Estado inexistente
        mvc.perform(get("/api/admin/reservas").param("estado", "XYZ"))
                .andExpect(status().isBadRequest());
    }
}
