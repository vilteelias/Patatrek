package com.patatrek.backend.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envío de correos (RNF-22).
 * Si no hay servidor de correo configurado (spring.mail.host), el correo se "simula":
 * se escribe en la consola del backend. Así el sistema funciona igual en desarrollo y en la demo.
 * Un error al enviar nunca interrumpe la operación que lo dispara.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remitente;

    public NotificacionService(ObjectProvider<JavaMailSender> mailSender,
                               @Value("${spring.mail.username:no-reply@patatrek.com}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    public void enviar(String destinatario, String asunto, String cuerpo) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("[Correo simulado] Para: {} | Asunto: {}\n{}", destinatario, asunto, cuerpo);
            return;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            sender.send(mensaje);
        } catch (MailException ex) {
            log.warn("No se pudo enviar el correo a {}: {}", destinatario, ex.getMessage());
        }
    }
}
