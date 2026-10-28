package com.patatrek.backend.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** CU-01 Registrar cliente, CU-02 Iniciar sesión, CU-03 Cerrar sesión (rutas sin bloquear). */
@SpringBootTest
@ActiveProfiles("demo")
@TestPropertySource(properties = {"patatrek.carpeta-uploads=target/test-uploads",
        "patatrek.revision-vencimientos-ms=600000"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthApiTest {

    static final String CLAVE_DEMO = "Patatrek2026";

    @Autowired WebApplicationContext contexto;

    MockMvc mvc;

    @BeforeEach
    void prepararMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private MockHttpSession login(String correo, String clave) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correoElectronico\":\"" + correo + "\",\"contrasenia\":\"" + clave + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    void loginConDatosIncorrectosOIncompletos() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correoElectronico\":\"sofia.ibarra@mail.com\",\"contrasenia\":\"otra\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje", containsString("incorrectos")));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.correoElectronico").exists())
                .andExpect(jsonPath("$.errores.contrasenia").exists());
    }

    @Test
    void cadaRolIniciaSesionYSeUsaSuIdentidad() throws Exception {
        MockHttpSession admin = login("admin@patatrek.com", CLAVE_DEMO);
        mvc.perform(get("/api/auth/sesion").session(admin))
                .andExpect(jsonPath("$.usuario.rol").value("ADMINISTRADOR"));

        // Brenda Coronel (guía) ve SUS salidas, no las del guía de demostración
        MockHttpSession guia = login("brenda.coronel@patatrek.com", CLAVE_DEMO);
        mvc.perform(get("/api/guia/yo").session(guia))
                .andExpect(jsonPath("$.apellido").value("Coronel"));

        MockHttpSession cliente = login("SOFIA.IBARRA@mail.com", CLAVE_DEMO);
        mvc.perform(get("/api/auth/sesion").session(cliente))
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.usuario.nombre").value("Sofía"));

        // CU-03 Cerrar sesión
        mvc.perform(post("/api/auth/logout").session(cliente)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/sesion"))
                .andExpect(jsonPath("$.usuario").value(nullValue()));
    }

    @Test
    void registroValidaCamposObligatoriosYDuplicados() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"contrasenia\":\"corta\",\"confirmarContrasenia\":\"otra\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.apellido").exists())
                .andExpect(jsonPath("$.errores.dni").exists())
                .andExpect(jsonPath("$.errores.correoElectronico").exists())
                .andExpect(jsonPath("$.errores.telefono").exists())
                .andExpect(jsonPath("$.errores.fechaNacimiento").exists())
                .andExpect(jsonPath("$.errores.contrasenia", containsString("8 caracteres")))
                .andExpect(jsonPath("$.errores.confirmarContrasenia").value("Las contraseñas no coinciden."))
                .andExpect(jsonPath("$.errores.nombre").doesNotExist());

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("38555777", "sofia.ibarra@mail.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.dni").exists())
                .andExpect(jsonPath("$.errores.correoElectronico").exists());
    }

    @Test
    void elClienteRegistradoQuedaConSesionYVeSusDatos() throws Exception {
        MockHttpSession sesion = (MockHttpSession) mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON).content(registro("40123456", "nuevo@mail.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"))
                .andReturn().getRequest().getSession(false);

        mvc.perform(get("/api/cliente/yo").session(sesion))
                .andExpect(jsonPath("$.correoElectronico").value("nuevo@mail.com"));
        mvc.perform(get("/api/cliente/reservas").session(sesion))
                .andExpect(jsonPath("$", hasSize(0)));

        // Y puede volver a ingresar con su contraseña
        login("nuevo@mail.com", "Montania2026");
    }

    private static String registro(String dni, String correo) {
        return """
                {"nombre":"Nicolás","apellido":"Prueba","dni":"%s","correoElectronico":"%s","telefono":"11-2233-4455",
                 "fechaNacimiento":"1995-03-10","contrasenia":"Montania2026","confirmarContrasenia":"Montania2026"}"""
                .formatted(dni, correo);
    }
}
