package com.patatrek.backend.notificacion;

import jakarta.annotation.PreDestroy;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Envío de correos (RNF-22) con la funcionalidad de correo del framework (Spring Mail + SMTP),
 * como indican el ERS y el Documento de Arquitectura.
 *
 * Cómo se comporta:
 *  - Sin credenciales (variables de entorno MAIL_USERNAME y MAIL_PASSWORD vacías) el correo se "simula":
 *    se muestra en la consola del backend como [Correo simulado]. Así funciona igual en desarrollo y en los tests.
 *  - Los correos a "dominios de prueba" (por defecto mail.com y patatrek.com, los de los datos de ejemplo)
 *    tampoco se mandan: así nunca le llega un correo a una persona real que tenga esa dirección.
 *  - Se envía DESPUÉS de que la operación se guarda (si la transacción falla, no sale ningún correo)
 *    y en segundo plano, para que el usuario no espere al servidor de correo (RNF-04).
 *  - Un error al enviar nunca interrumpe la operación que lo dispara: queda en el log.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final PlantillaCorreo plantilla;
    private final RegistroCorreos registro;
    private final JavaMailSenderImpl sender;
    private final String remitente;
    private final String nombreRemitente;
    private final Set<String> dominiosPrueba;
    private final boolean asincronico;
    private final ExecutorService ejecutor;
    private final String urlFrontend;

    public NotificacionService(PlantillaCorreo plantilla, RegistroCorreos registro,
                               @Value("${patatrek.correo.host:smtp.gmail.com}") String host,
                               @Value("${patatrek.correo.puerto:587}") int puerto,
                               @Value("${patatrek.correo.usuario:${MAIL_USERNAME:}}") String usuario,
                               @Value("${patatrek.correo.contrasenia:${MAIL_PASSWORD:}}") String contrasenia,
                               @Value("${patatrek.correo.nombre-remitente:PataTrek}") String nombreRemitente,
                               @Value("${patatrek.correo.dominios-prueba:mail.com,patatrek.com}") String dominiosPrueba,
                               @Value("${patatrek.correo.asincronico:true}") boolean asincronico,
                               @Value("${patatrek.frontend-url:http://localhost:5173}") String urlFrontend) {
        this.urlFrontend = urlFrontend.replaceAll("/+$", "");
        this.plantilla = plantilla;
        this.registro = registro;
        this.remitente = usuario == null ? "" : usuario.trim();
        this.nombreRemitente = nombreRemitente;
        this.dominiosPrueba = Arrays.stream(dominiosPrueba.split(","))
                .map(d -> d.trim().toLowerCase(Locale.ROOT)).filter(d -> !d.isEmpty()).collect(Collectors.toSet());
        this.asincronico = asincronico;
        this.ejecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "envio-correos");
            t.setDaemon(true);
            return t;
        });

        if (this.remitente.isEmpty() || contrasenia == null || contrasenia.isBlank()) {
            this.sender = null;
            log.info("Correo: sin credenciales (MAIL_USERNAME / MAIL_PASSWORD). Los correos se muestran en la consola.");
        } else {
            JavaMailSenderImpl s = new JavaMailSenderImpl();
            s.setHost(host);
            s.setPort(puerto);
            s.setUsername(this.remitente);
            s.setPassword(contrasenia.replace(" ", "")); // la contraseña de aplicación de Gmail se copia con espacios
            s.setDefaultEncoding("UTF-8");
            Properties p = s.getJavaMailProperties();
            p.put("mail.transport.protocol", "smtp");
            p.put("mail.smtp.auth", "true");
            p.put("mail.smtp.starttls.enable", "true");
            p.put("mail.smtp.starttls.required", "true");
            p.put("mail.smtp.connectiontimeout", "10000");
            p.put("mail.smtp.timeout", "10000");
            p.put("mail.smtp.writetimeout", "10000");
            this.sender = s;
            log.info("Correo: se envía por {}:{} como {}. Dominios de prueba (no se envían): {}",
                    host, puerto, this.remitente, this.dominiosPrueba);
        }
    }

    /** Dirección completa de una pantalla del frontend, para los botones de los correos. */
    public String enlace(String ruta) {
        return urlFrontend + (ruta.startsWith("/") ? ruta : "/" + ruta);
    }

    /** Correo armado a partir de texto libre: cada párrafo separado por una línea en blanco. */
    public void enviar(String destinatario, String asunto, String cuerpo) {
        enviar(Correo.deTextoLibre(destinatario, asunto, cuerpo));
    }

    public void enviar(Correo correo) {
        if (correo.destinatario() == null || correo.destinatario().isBlank()) {
            log.debug("Correo sin destinatario, no se envía: {}", correo.asunto());
            return;
        }
        Runnable tarea = () -> entregar(correo);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // Solo si la operación se guarda: si la transacción se deshace, el correo no sale.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    despachar(tarea);
                }
            });
        } else {
            despachar(tarea);
        }
    }

    private void despachar(Runnable tarea) {
        if (asincronico) {
            ejecutor.execute(tarea);
        } else {
            tarea.run();
        }
    }

    private void entregar(Correo c) {
        if (sender == null) {
            simular(c, "[Correo simulado]");
            return;
        }
        if (esDominioDePrueba(c.destinatario())) {
            simular(c, "[Correo simulado: dominio de prueba]");
            return;
        }
        try {
            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper m = new MimeMessageHelper(mensaje, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
            m.setFrom(remitente, nombreRemitente);
            m.setTo(c.destinatario().trim());
            m.setSubject(c.asunto());
            m.setText(plantilla.texto(c), plantilla.html(c, true));
            m.addInline(PlantillaCorreo.CID_LOGO, new ClassPathResource("correo/logo.png"), "image/png");
            sender.send(mensaje);
            registro.registrar(c, RegistroCorreos.Resultado.ENVIADO, null);
            log.info("Correo enviado a {} | {}", c.destinatario(), c.asunto());
        } catch (Exception ex) {
            registro.registrar(c, RegistroCorreos.Resultado.ERROR, ex.getMessage());
            log.warn("No se pudo enviar el correo a {} ({}): {}", c.destinatario(), c.asunto(), ex.getMessage());
        }
    }

    private void simular(Correo c, String etiqueta) {
        registro.registrar(c, RegistroCorreos.Resultado.SIMULADO, etiqueta);
        log.info("{} Para: {} | Asunto: {}\n{}", etiqueta, c.destinatario(), c.asunto(), plantilla.texto(c));
    }

    private boolean esDominioDePrueba(String direccion) {
        int arroba = direccion.lastIndexOf('@');
        if (arroba < 0) return false;
        String dominio = direccion.substring(arroba + 1).trim().toLowerCase(Locale.ROOT);
        return dominiosPrueba.stream().anyMatch(d -> dominio.equals(d) || dominio.endsWith("." + d));
    }

    @PreDestroy
    void cerrar() {
        ejecutor.shutdown();
    }
}
