package com.patatrek.backend.contingencia.estrategias;

import com.patatrek.backend.contingencia.*;
import com.patatrek.backend.contingencia.dto.ContingenciaRequest;
import com.patatrek.backend.notificacion.Correo;
import com.patatrek.backend.notificacion.FormatoCorreo;
import com.patatrek.backend.notificacion.NotificacionService;
import com.patatrek.backend.reserva.*;
import com.patatrek.backend.salida.GestionSalidaService;
import com.patatrek.backend.salida.Salida;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class ModificacionStrategy implements ResolucionContingenciaStrategy {

    private final GestionSalidaService gestionSalida;
    private final ParticipanteRepository participanteRepository;
    private final NotificacionService notificacionService;

    public ModificacionStrategy(GestionSalidaService gestionSalida, ParticipanteRepository participanteRepository,
                                NotificacionService notificacionService) {
        this.gestionSalida = gestionSalida;
        this.participanteRepository = participanteRepository;
        this.notificacionService = notificacionService;
    }

    @Override
    public TipoResolucion getTipoResolucion() {
        return TipoResolucion.MODIFICACION;
    }

    @Override
    public ResultadoContingencia procesar(Salida s, Contingencia c, ContingenciaRequest d) {
        String recorrido = d.nuevoRecorrido() != null ? d.nuevoRecorrido().trim() : "";
        s.setRecorrido(recorrido);
        c.setRecorridoAlternativo(recorrido);
        int afectadas = 0;
        int correos = 0;
        String motivo = c.getMotivo();

        for (Reserva r : gestionSalida.reservasQueOcupan(s.getIdSalida())) {
            if (!EstadoReserva.VIGENTES.contains(r.getEstadoReserva())) continue;
            afectadas++;
            correos += avisar(r, s, c, recorrido, motivo, destinatario -> Correo.para(destinatario)
                    .asunto("PataTrek — Nuevo recorrido para tu salida de " + s.getExcursion().getNombre())
                    .titulo("Cambió el recorrido de tu salida")
                    .parrafo("Por una contingencia " + TipoContingencia.valueOf(c.getTipoContingencia()).texto() + " ajustamos el recorrido. La fecha y el horario no cambian.")
                    .dato("Excursión", s.getExcursion().getNombre())
                    .dato("Salida", FormatoCorreo.salida(s.getFechaInicio(), s.getFechaFinalizacion(), s.getHoraInicio()))
                    .dato("Reserva", "#" + r.getIdReserva())
                    .dato("Motivo", motivo)
                    .datoDestacado("Nuevo recorrido", recorrido)
                    .nota("Tu reserva sigue vigente. Este cambio no modifica el cronograma, por eso no da derecho a devolución."));
        }
        String mensaje = "Se actualizó el recorrido. "
                + (afectadas == 0 ? "No tenía reservas vigentes."
                : "Se avisó por correo a " + (afectadas == 1 ? "1 reserva." : afectadas + " reservas."));
        return new ResultadoContingencia(mensaje, afectadas, correos);
    }

    private int avisar(Reserva r, Salida s, Contingencia c, String recorrido, String motivo, java.util.function.Function<String, Correo.Builder> armar) {
        Map<String, String> destinatarios = new LinkedHashMap<>();
        destinatarios.put(r.getCliente().getCorreoElectronico().trim().toLowerCase(Locale.ROOT), r.getCliente().getNombre());
        for (Participante p : participanteRepository.findByReservaIdReservaOrderByIdParticipanteAsc(r.getIdReserva())) {
            if (p.getCorreoElectronico() != null && !p.getCorreoElectronico().isBlank()) {
                destinatarios.putIfAbsent(p.getCorreoElectronico().trim().toLowerCase(Locale.ROOT), p.getNombre());
            }
        }
        destinatarios.forEach((correo, nombre) ->
                notificacionService.enviar(armar.apply(correo).saludo("Hola " + nombre + ":").build()));
        return destinatarios.size();
    }
}
