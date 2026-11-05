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
import com.patatrek.backend.salida.EstadoSalida;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class CancelacionStrategy implements ResolucionContingenciaStrategy {

    private final GestionSalidaService gestionSalida;
    private final ReservaService reservaService;
    private final ParticipanteRepository participanteRepository;
    private final NotificacionService notificacionService;

    public CancelacionStrategy(GestionSalidaService gestionSalida, ReservaService reservaService,
                               ParticipanteRepository participanteRepository, NotificacionService notificacionService) {
        this.gestionSalida = gestionSalida;
        this.reservaService = reservaService;
        this.participanteRepository = participanteRepository;
        this.notificacionService = notificacionService;
    }

    @Override
    public TipoResolucion getTipoResolucion() {
        return TipoResolucion.CANCELACION;
    }

    @Override
    public ResultadoContingencia procesar(Salida s, Contingencia c, ContingenciaRequest d) {
        TipoContingencia tipo = TipoContingencia.valueOf(c.getTipoContingencia());
        String motivo = c.getMotivo();
        String motivoReserva = "Cancelación de la salida por contingencia " + tipo.texto() + ": " + motivo;
        int afectadas = 0;
        int correos = 0;

        for (Reserva r : gestionSalida.reservasQueOcupan(s.getIdSalida())) {
            if (r.getEstadoReserva() == EstadoReserva.PENDIENTE_PAGO) {
                reservaService.cancelarPendientePorContingencia(r, motivoReserva);
                continue;
            }
            reservaService.liberarRecursosConsolidados(r, s);
            r.setEstadoReserva(EstadoReserva.PENDIENTE_DEVOLUCION);
            r.setReprogramacionPendiente(false);
            r.setMotivoCancelacion(motivoReserva);
            afectadas++;
            correos += avisar(r, s, tipo, motivo, destinatario -> Correo.para(destinatario)
                    .asunto("PataTrek — Se canceló tu salida de " + s.getExcursion().getNombre())
                    .titulo("Tu salida fue cancelada")
                    .parrafo("Por una contingencia " + tipo.texto() + " tuvimos que cancelar la salida. Lamentamos los inconvenientes.")
                    .dato("Excursión", s.getExcursion().getNombre())
                    .dato("Salida", FormatoCorreo.salida(s.getFechaInicio(), s.getFechaFinalizacion(), s.getHoraInicio()))
                    .dato("Reserva", "#" + r.getIdReserva())
                    .dato("Motivo", motivo)
                    .nota("Ya iniciamos el trámite de devolución total de lo abonado. Cuando se procese, el titular de la reserva recibirá el comprobante por correo."));
        }
        s.setEstadoSalida(EstadoSalida.CANCELADA);
        s.setMotivoCancelacion("Contingencia " + tipo.texto() + ": " + motivo);
        gestionSalida.liberarGuiasYRefugios(s);

        String mensaje = "La salida quedó Cancelada. "
                + (afectadas == 0 ? "No tenía reservas vigentes."
                : afectadas == 1 ? "1 reserva pasó a Pendiente de devolución y se avisó por correo."
                : afectadas + " reservas pasaron a Pendiente de devolución y se avisó por correo.");
        return new ResultadoContingencia(mensaje, afectadas, correos);
    }

    private int avisar(Reserva r, Salida s, TipoContingencia tipo, String motivo, java.util.function.Function<String, Correo.Builder> armar) {
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
