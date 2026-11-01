package com.patatrek.backend.reserva;

import com.patatrek.backend.certificado.CertificadoMedico;
import com.patatrek.backend.certificado.CertificadoMedicoRepository;
import com.patatrek.backend.certificado.EstadoCertificado;
import com.patatrek.backend.notificacion.RegistroCorreos;
import com.patatrek.backend.salida.Salida;
import com.patatrek.backend.salida.SalidaRepository;
import com.patatrek.backend.usuario.Cliente;
import com.patatrek.backend.usuario.ClienteRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CU-30 Cancelar reservas automáticamente. Usa la salida de ejemplo del Perito Moreno de dentro de 2 días
 * (exige certificado médico) y le agrega tres reservas: una señada, una confirmada sin certificados
 * y una confirmada con un certificado cargado.
 */
@SpringBootTest
@ActiveProfiles("demo")
@TestPropertySource(properties = {"patatrek.carpeta-uploads=target/test-uploads",
        "patatrek.revision-vencimientos-ms=600000",
        "patatrek.pasarela.permitir-forzar-resultado=true", "patatrek.correo.asincronico=false"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CancelacionAutomaticaApiTest {

    @Autowired WebApplicationContext contexto;
    @Autowired TransactionTemplate tx;
    @Autowired SalidaRepository salidaRepository;
    @Autowired ReservaRepository reservaRepository;
    @Autowired ParticipanteRepository participanteRepository;
    @Autowired ClienteRepository clienteRepository;
    @Autowired CertificadoMedicoRepository certificadoRepository;
    @Autowired RegistroCorreos registroCorreos;

    MockMvc mvc;

    @BeforeEach
    void prepararMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private Salida peritoEnDosDias() {
        LocalDate dia = LocalDate.now().plusDays(2);
        return salidaRepository.findAll().stream()
                .filter(s -> s.getExcursion().getNombre().equals("Ascenso Cerro Perito Moreno")
                        && s.getFechaInicio().equals(dia))
                .findFirst().orElseThrow();
    }

    private Reserva reserva(Salida s, String dni, String nombre, EstadoReserva estado, int cantidad, String saldo) {
        Cliente c = clienteRepository.save(new Cliente(dni, nombre, "Prueba", nombre.toLowerCase() + "@ejemplo.com",
                "11-2233-4455", LocalDate.of(1990, 1, 1)));
        Reserva r = new Reserva();
        r.setSalida(s);
        r.setCliente(c);
        r.setFechaHora(LocalDateTime.now().minusDays(10));
        r.setEstadoReserva(estado);
        r.setCantidadParticipantes(cantidad);
        r.setMontoTotal(new BigDecimal("95000").multiply(BigDecimal.valueOf(cantidad)));
        r.setSaldoPendiente(new BigDecimal(saldo));
        r = reservaRepository.save(r);
        s.setCuposOcupados(s.getCuposOcupados() + cantidad);
        return r;
    }

    private Participante participante(Reserva r, String dni, String nombre, EstadoCertificado certificado) {
        Participante p = participanteRepository.save(new Participante(r, dni, nombre, "Prueba",
                nombre.toLowerCase() + "@ejemplo.com", "11-2233-4455", LocalDate.of(1990, 1, 1)));
        if (certificado != null) {
            certificadoRepository.save(new CertificadoMedico(p, "certificados/prueba-" + dni + ".pdf",
                    LocalDateTime.now().minusDays(3), LocalDate.now().plusMonths(6), certificado));
        }
        return p;
    }

    @Test
    void cancelaLasQueIncumplenYLiberaLosCupos() throws Exception {
        long[] ids = tx.execute(estado -> {
            Salida s = peritoEnDosDias();
            // Señada: adeuda el saldo (aunque tenga certificado cargado)
            Reserva senada = reserva(s, "31000001", "Ana", EstadoReserva.SENADA, 1, "47500");
            participante(senada, "31000001", "Ana", EstadoCertificado.CARGADO);
            // Confirmada sin ningún certificado válido (pendiente y rechazado)
            Reserva sinCertificado = reserva(s, "31000002", "Beto", EstadoReserva.CONFIRMADA, 2, "0");
            participante(sinCertificado, "31000002", "Beto", EstadoCertificado.PENDIENTE);
            participante(sinCertificado, "31000003", "Carla", EstadoCertificado.RECHAZADO);
            // Confirmada con un certificado cargado: NO se cancela (regla de negocio del CU-30)
            Reserva conUno = reserva(s, "31000004", "Dario", EstadoReserva.CONFIRMADA, 2, "0");
            participante(conUno, "31000004", "Dario", EstadoCertificado.CARGADO);
            participante(conUno, "31000005", "Eva", null);
            salidaRepository.save(s);
            return new long[]{s.getIdSalida(), s.getCuposOcupados(), senada.getIdReserva(),
                    sinCertificado.getIdReserva(), conUno.getIdReserva()};
        });
        long idSalida = ids[0];
        int ocupadosAntes = (int) ids[1];
        registroCorreos.limpiar();

        mvc.perform(post("/api/admin/reservas/cancelacion-automatica"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canceladas", hasSize(2)))
                .andExpect(jsonPath("$.canceladas[*].idReserva", containsInAnyOrder((int) ids[2], (int) ids[3])))
                .andExpect(jsonPath("$.canceladas[?(@.idReserva == " + ids[2] + ")].motivo",
                        contains(containsString("saldo restante"))))
                .andExpect(jsonPath("$.canceladas[?(@.idReserva == " + ids[3] + ")].motivo",
                        contains(containsString("certificado médico"))))
                .andExpect(jsonPath("$.mensaje", containsString("Se cancelaron 2 reservas")));

        assertThat(reservaRepository.findById(ids[2]).orElseThrow().getEstadoReserva()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(reservaRepository.findById(ids[3]).orElseThrow().getMotivoCancelacion()).startsWith("Cancelación automática");
        assertThat(reservaRepository.findById(ids[4]).orElseThrow().getEstadoReserva()).isEqualTo(EstadoReserva.CONFIRMADA);
        // Se liberaron los 3 cupos de las dos reservas canceladas
        assertThat(salidaRepository.findById(idSalida).orElseThrow().getCuposOcupados()).isEqualTo(ocupadosAntes - 3);

        // Paso 6: correo a cada titular cancelado, y a nadie más
        assertThat(registroCorreos.para("ana@ejemplo.com")).singleElement()
                .satisfies(e -> assertThat(e.correo().asunto()).contains("fue cancelada"));
        assertThat(registroCorreos.para("beto@ejemplo.com")).hasSize(1);
        assertThat(registroCorreos.para("dario@ejemplo.com")).isEmpty();

        // Flujo 5a: la segunda ejecución no encuentra nada para cancelar
        mvc.perform(post("/api/admin/reservas/cancelacion-automatica"))
                .andExpect(jsonPath("$.canceladas", hasSize(0)))
                .andExpect(jsonPath("$.mensaje", containsString("no se canceló ninguna")));
    }

    @Test
    void conLosDatosDeEjemploNoCancelaNada() throws Exception {
        mvc.perform(post("/api/admin/reservas/cancelacion-automatica"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canceladas", hasSize(0)));
    }
}
