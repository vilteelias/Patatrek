package com.patatrek.backend.correo;

import org.springframework.stereotype.Service;

@Service
public class ServicioCorreo {

    public void enviarMailCancelacion(String destinatario, Long idReserva, String motivo) {
        System.out.println("📧 [SIMULADOR MAIL] Enviando correo a: " + destinatario);
        System.out.println("   Asunto: Tu reserva #" + idReserva + " ha sido cancelada.");
        System.out.println("   Motivo: " + motivo);
    }

    public void enviarMailReprogramacion(String destinatario, Long idReserva) {
        System.out.println("📧 [SIMULADOR MAIL] Enviando correo a: " + destinatario);
        System.out.println("   Asunto: Tu reserva #" + idReserva + " ha sido reprogramada.");
        System.out.println("   Mensaje: Por favor revisa tu panel para aceptar o rechazar la nueva fecha.");
    }

    public void enviarMailNuevoItinerario(String destinatario, Long idReserva) {
        System.out.println("📧 [SIMULADOR MAIL] Enviando correo a: " + destinatario);
        System.out.println("   Asunto: Modificación en el recorrido de tu reserva #" + idReserva);
        System.out.println("   Mensaje: El itinerario ha sido ajustado. Revisa los detalles en tu cuenta.");
    }
}
