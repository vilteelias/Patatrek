package com.patatrek.backend.certificado.dto;

import com.patatrek.backend.certificado.CertificadoMedico;
import com.patatrek.backend.reserva.Participante;
import com.patatrek.backend.reserva.Reserva;
import com.patatrek.backend.salida.Salida;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Una fila de la tabla "Validar certificados médicos" del Administrador (CU-25). */
public record CertificadoValidacionResponse(
        Long idCertificadoMedico,
        String nombre,
        String apellido,
        String dni,
        Long idReserva,
        String nombreExcursion,
        LocalDate fechaInicioSalida,
        LocalDate fechaFinalizacionSalida,
        LocalDateTime fechaSubida,
        String nombreArchivo,
        int intentosEnvio
) {
    public static CertificadoValidacionResponse de(CertificadoMedico c, String nombreArchivo) {
        Participante p = c.getParticipante();
        Reserva r = p.getReserva();
        Salida s = r.getSalida();
        return new CertificadoValidacionResponse(
                c.getIdCertificadoMedico(), p.getNombre(), p.getApellido(), p.getDni(),
                r.getIdReserva(), s.getExcursion().getNombre(), s.getFechaInicio(), s.getFechaFinalizacion(),
                c.getFechaSubida(), nombreArchivo,
                c.getIntentosEnvio() == null ? 0 : c.getIntentosEnvio());
    }
}
