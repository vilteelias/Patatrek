package com.patatrek.backend.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Con patatrek.seguridad.exigir-login=true cada rol solo accede a sus rutas. */
@SpringBootTest
@ActiveProfiles("demo")
@TestPropertySource(properties = {"patatrek.carpeta-uploads=target/test-uploads",
        "patatrek.revision-vencimientos-ms=600000", "patatrek.seguridad.exigir-login=true"})
class AuthEstrictoApiTest {

    @Autowired WebApplicationContext contexto;

    MockMvc mvc;

    @BeforeEach
    void prepararMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private MockHttpSession login(String correo) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correoElectronico\":\"" + correo + "\",\"contrasenia\":\"Patatrek2026\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    void cadaRolSoloEntraASusRutas() throws Exception {
        mvc.perform(get("/api/excursiones")).andExpect(status().isOk());          // catálogo público
        mvc.perform(get("/api/admin/reservas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cliente/reservas")).andExpect(status().isUnauthorized());

        MockHttpSession cliente = login("sofia.ibarra@mail.com");
        mvc.perform(get("/api/cliente/reservas").session(cliente)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/reservas").session(cliente)).andExpect(status().isForbidden());

        MockHttpSession admin = login("admin@patatrek.com");
        mvc.perform(get("/api/admin/reservas").session(admin)).andExpect(status().isOk());
        mvc.perform(get("/api/guia/salidas").session(admin)).andExpect(status().isForbidden());

        MockHttpSession guia = login("martin.almeida@patatrek.com");
        mvc.perform(get("/api/guia/salidas").session(guia)).andExpect(status().isOk());
    }
}
