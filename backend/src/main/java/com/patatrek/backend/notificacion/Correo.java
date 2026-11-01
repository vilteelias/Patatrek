package com.patatrek.backend.notificacion;

import java.util.ArrayList;
import java.util.List;

/**
 * Contenido de un correo de PataTrek, independiente de su formato.
 * La plantilla (PlantillaCorreo) lo convierte en HTML con el diseño de PataTrek y en texto plano.
 *
 * <pre>
 * Correo.para(cliente.getCorreoElectronico())
 *       .asunto("PataTrek — Comprobante de pago de la reserva #12")
 *       .titulo("¡Recibimos tu pago!")
 *       .saludo("Hola Sofía:")
 *       .parrafo("Este es el comprobante de tu pago.")
 *       .dato("Reserva", "#12")
 *       .boton("Ver mi reserva", url)
 *       .build();
 * </pre>
 */
public record Correo(
        String destinatario,
        String asunto,
        String titulo,
        String saludo,
        List<String> parrafos,
        List<Dato> datos,
        String textoBoton,
        String urlBoton,
        String nota
) {

    /** Una fila de la tabla de detalle (etiqueta: valor). destacado = se muestra en negrita y más grande. */
    public record Dato(String etiqueta, String valor, boolean destacado) {}

    public static Builder para(String destinatario) {
        return new Builder(destinatario);
    }

    public static final class Builder {
        private final String destinatario;
        private String asunto;
        private String titulo;
        private String saludo;
        private final List<String> parrafos = new ArrayList<>();
        private final List<Dato> datos = new ArrayList<>();
        private String textoBoton;
        private String urlBoton;
        private String nota;

        private Builder(String destinatario) {
            this.destinatario = destinatario;
        }

        public Builder asunto(String asunto) { this.asunto = asunto; return this; }
        public Builder titulo(String titulo) { this.titulo = titulo; return this; }
        public Builder saludo(String saludo) { this.saludo = saludo; return this; }
        public Builder parrafo(String parrafo) { this.parrafos.add(parrafo); return this; }
        public Builder dato(String etiqueta, String valor) { this.datos.add(new Dato(etiqueta, valor, false)); return this; }
        public Builder datoDestacado(String etiqueta, String valor) { this.datos.add(new Dato(etiqueta, valor, true)); return this; }
        public Builder boton(String texto, String url) { this.textoBoton = texto; this.urlBoton = url; return this; }
        /** Texto chico al final (condiciones, aclaraciones). */
        public Builder nota(String nota) { this.nota = nota; return this; }

        public Correo build() {
            return new Correo(destinatario, asunto, titulo == null ? asunto : titulo, saludo,
                    List.copyOf(parrafos), List.copyOf(datos), textoBoton, urlBoton, nota);
        }
    }

    /** Correo armado a partir de texto libre (el método enviar(destinatario, asunto, cuerpo) de siempre). */
    public static Correo deTextoLibre(String destinatario, String asunto, String cuerpo) {
        Builder b = para(destinatario).asunto(asunto)
                .titulo(asunto.replaceFirst("^PataTrek\\s*[—-]\\s*", ""));
        for (String bloque : (cuerpo == null ? "" : cuerpo.strip()).split("\\R\\s*\\R")) {
            if (!bloque.isBlank()) b.parrafo(bloque.strip());
        }
        return b.build();
    }
}
