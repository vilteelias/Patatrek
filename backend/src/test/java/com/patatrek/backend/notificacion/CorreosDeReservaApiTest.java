package com.patatrek.backend.notificacion;

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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RNF-22: qué correos salen (y cuáles no) en el flujo de reserva y pago. */
@SpringBootTest
@ActiveProfiles("demo")
@TestPropertySource(properties = {"patatrek.carpeta-uploads=target/test-uploads",
        "patatrek.revision-vencimientos-ms=600000",
        "patatrek.pasarela.permitir-forzar-resultado=true", "patatrek.correo.asincronico=false"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CorreosDeReservaApiTest {

    static final String SOFIA = "sofia.ibarra@mail.com";

    @Autowired WebApplicationContext contexto;
    @Autowired ExcursionRepository excursionRepository;
    @Autowired RegistroCorreos registro;
    @Autowired NotificacionService notificacionService;
    @Autowired TransactionTemplate tx;

    MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        registro.limpiar();
    }

    private long reservaLaguna() throws Exception {
        return reservar("Sendero Laguna del Bosque", """
                {"dni":"30111222","nombre":"Ana","apellido":"Prueba","correoElectronico":"ana@ejemplo.com",
                 "nroTelefono":"11-2233-4455","fechaNacimiento":"1990-01-01"}""");
    }

    private long reservar(String excursion, String participantes) throws Exception {
        long idExcursion = excursionRepository.findAllByOrderByNombreAsc().stream()
                .filter(e -> e.getNombre().equals(excursion)).findFirst().orElseThrow().getIdExcursion();
        String salidas = mvc.perform(get("/api/excursiones/" + idExcursion + "/salidas")).andReturn().getResponse().getContentAsString();
        int idSalida = JsonPath.<List<Integer>>read(salidas, "$[?(@.reservable == true)].idSalida").get(0);
        String json = mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idSalida\":" + idSalida + ",\"participantes\":[" + participantes + "]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Integer) JsonPath.read(json, "$.idReserva")).longValue();
    }

    private void pagar(long id, String modo, boolean aprobar) throws Exception {
        mvc.perform(post("/api/reservas/" + id + "/pagos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modoPago\":\"" + modo + "\",\"aprobar\":" + aprobar + "}"))
                .andExpect(status().isOk());
    }

    @Test
    void elPagoAprobadoMandaElComprobanteConElDetalle() throws Exception {
        long id = reservaLaguna();
        assertThat(registro.para(SOFIA)).as("registrar la reserva no manda correo").isEmpty();
        pagar(id, "SENA", true);

        assertThat(registro.para(SOFIA)).singleElement().satisfies(e -> {
            Correo c = e.correo();
            assertThat(c.asunto()).contains("Comprobante de pago de la reserva #" + id);
            assertThat(c.datos()).extracting(Correo.Dato::etiqueta)
                    .contains("Modo de pago", "N.º de transacción", "Importe abonado", "Saldo pendiente");
            assertThat(c.urlBoton()).endsWith("/reservas/" + id);
        });
    }

    @Test
    void elPagoRechazadoNoMandaCorreo() throws Exception {
        long id = reservaLaguna();
        pagar(id, "SENA", false);
        assertThat(registro.para(SOFIA)).isEmpty();
    }

    @Test
    void laCancelacionDelClienteMandaElDetalle() throws Exception {
        long id = reservaLaguna();
        pagar(id, "TOTAL", true);
        registro.limpiar();
        mvc.perform(post("/api/reservas/" + id + "/cancelacion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Cambio de planes\"}")).andExpect(status().isOk());
        assertThat(registro.para(SOFIA)).singleElement().satisfies(e -> {
            assertThat(e.correo().asunto()).contains("Cancelaste la reserva #" + id);
            assertThat(e.correo().datos()).anySatisfy(d -> assertThat(d.valor()).isEqualTo("Cambio de planes"));
            assertThat(e.correo().nota()).contains("no se devuelve");
        });
    }

    @Test
    void siLaOperacionFallaNoSaleNingunCorreo() {
        try {
            tx.executeWithoutResult(estado -> {
                notificacionService.enviar("alguien@ejemplo.com", "Asunto", "Cuerpo");
                throw new IllegalStateException("falla después de pedir el correo");
            });
        } catch (IllegalStateException esperado) {
            // la transacción se deshizo
        }
        assertThat(registro.para("alguien@ejemplo.com")).isEmpty();

        tx.executeWithoutResult(estado -> notificacionService.enviar("alguien@ejemplo.com", "Asunto", "Cuerpo"));
        assertThat(registro.para("alguien@ejemplo.com")).hasSize(1);
    }

    @Test
    void siLaExcursionExigeCertificadoCadaParticipanteRecibeSuEnlace() throws Exception {
        // Perito Moreno exige certificado. Un participante con el correo del titular y otro con correo propio.
        long id = reservar("Ascenso Cerro Perito Moreno", """
                {"dni":"38555777","nombre":"Sofía","apellido":"Ibarra","correoElectronico":"sofia.ibarra@mail.com",
                 "nroTelefono":"2804-667788","fechaNacimiento":"1994-03-22"},
                {"dni":"30111223","nombre":"Beto","apellido":"Prueba","correoElectronico":"beto@ejemplo.com",
                 "nroTelefono":"11-2233-4455","fechaNacimiento":"1990-01-01"}""");
        assertThat(registro.para("beto@ejemplo.com")).as("antes de pagar no se avisa").isEmpty();

        pagar(id, "SENA", true);
        assertThat(registro.para("beto@ejemplo.com")).singleElement().satisfies(e -> {
            assertThat(e.correo().asunto()).contains("Cargá tu certificado médico");
            assertThat(e.correo().urlBoton()).endsWith("/participante/reservas/" + id + "?dni=30111223");
            assertThat(e.correo().datos()).extracting(Correo.Dato::etiqueta).contains("Plazo para cargarlo");
        });
        // El titular recibe solo el comprobante (carga los certificados desde su cuenta)
        assertThat(registro.para(SOFIA)).singleElement()
                .satisfies(e -> assertThat(e.correo().asunto()).contains("Comprobante de pago"));

        // Pagar el saldo no vuelve a mandar el enlace
        pagar(id, "SALDO", true);
        assertThat(registro.para("beto@ejemplo.com")).hasSize(1);
    }

    @Test
    void siLaExcursionNoExigeCertificadoNoSeAvisaALosParticipantes() throws Exception {
        long id = reservaLaguna();
        pagar(id, "TOTAL", true);
        assertThat(registro.para("ana@ejemplo.com")).isEmpty();
    }
}
