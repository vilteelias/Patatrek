package com.patatrek.backend.notificacion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** Comportamiento del servicio de correo sin levantar Spring. */
class NotificacionServiceTest {

    private NotificacionService servicio(RegistroCorreos registro, String usuario, String contrasenia) {
        // Puerto 1 de localhost: no hay servidor, así que un envío real falla rápido
        return new NotificacionService(new PlantillaCorreo(), registro, "localhost", 1, usuario, contrasenia,
                "PataTrek", "mail.com,patatrek.com", false, "http://localhost:5173/");
    }

    @Test
    void sinCredencialesSeSimula() {
        RegistroCorreos registro = new RegistroCorreos();
        servicio(registro, "", "").enviar("alguien@gmail.com", "PataTrek — Prueba", "Hola.\n\nSegundo párrafo.");
        assertThat(registro.todos()).singleElement().satisfies(e -> {
            assertThat(e.resultado()).isEqualTo(RegistroCorreos.Resultado.SIMULADO);
            assertThat(e.correo().titulo()).isEqualTo("Prueba");
            assertThat(e.correo().parrafos()).containsExactly("Hola.", "Segundo párrafo.");
        });
    }

    @Test
    void losDominiosDePruebaNoSeEnvian() {
        RegistroCorreos registro = new RegistroCorreos();
        NotificacionService s = servicio(registro, "grupo@gmail.com", "clave");
        s.enviar("sofia.ibarra@mail.com", "Asunto", "Cuerpo");
        s.enviar("martin@PataTrek.com", "Asunto", "Cuerpo");
        assertThat(registro.todos()).hasSize(2)
                .allSatisfy(e -> assertThat(e.detalle()).contains("dominio de prueba"));
    }

    @Test
    void unErrorDelServidorDeCorreoNoInterrumpeLaOperacion() {
        RegistroCorreos registro = new RegistroCorreos();
        NotificacionService s = servicio(registro, "grupo@gmail.com", "clave");
        assertThatCode(() -> s.enviar("alguien@gmail.com", "Asunto", "Cuerpo")).doesNotThrowAnyException();
        assertThat(registro.todos()).singleElement()
                .satisfies(e -> assertThat(e.resultado()).isEqualTo(RegistroCorreos.Resultado.ERROR));
    }

    @Test
    void elEnlaceUsaLaDireccionDelFrontend() {
        assertThat(servicio(new RegistroCorreos(), "", "").enlace("/reservas/5"))
                .isEqualTo("http://localhost:5173/reservas/5");
    }

    @Test
    void laPlantillaEscapaElTextoYMuestraElBoton() {
        Correo c = Correo.para("a@b.com").asunto("Asunto").titulo("Hola <script>")
                .dato("Importe", "$ 95.000").boton("Ver mi reserva", "http://localhost:5173/reservas/1").build();
        PlantillaCorreo p = new PlantillaCorreo();
        String html = p.html(c, true);
        assertThat(html).contains("Hola &lt;script&gt;").doesNotContain("<script>")
                .contains("href=\"http://localhost:5173/reservas/1\"").contains("cid:" + PlantillaCorreo.CID_LOGO);
        assertThat(p.texto(c)).contains("Importe: $ 95.000").contains("Ver mi reserva: http://localhost:5173/reservas/1");
    }
}
