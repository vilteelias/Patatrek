package com.patatrek.backend.contingencia.estrategias;

import com.patatrek.backend.contingencia.*;
import com.patatrek.backend.salida.Salida;
import com.patatrek.backend.reserva.Reserva;
import com.patatrek.backend.reserva.ReservaRepository;
import com.patatrek.backend.correo.ServicioCorreo;
import org.springframework.stereotype.Component;
import java.util.List;

@Component("REPROGRAMACION")
public class ReprogramacionStrategy implements ResolucionContingenciaStrategy {

    private final ReservaRepository reservaRepository;
    private final ServicioCorreo servicioCorreo;

    public ReprogramacionStrategy(ReservaRepository reservaRepository, ServicioCorreo servicioCorreo) {
        this.reservaRepository = reservaRepository;
        this.servicioCorreo = servicioCorreo;
    }

    @Override
    public void procesarResolucion(Salida salida, Contingencia contingencia, ContingenciaDTO dto) {
        // Se actualiza la fecha en la salida
       salida.setFechaInicio(dto.getNuevaFecha());
        if (dto.getNuevoHorario() != null) {
        salida.setHoraInicio(dto.getNuevoHorario());
}
        // Si hay un recorrido alternativo (opcional), se guarda en la contingencia
        if (dto.getNuevoRecorrido() != null && !dto.getNuevoRecorrido().isEmpty()) {
            contingencia.setRecorridoAlternativo(dto.getNuevoRecorrido());
        }
        
        List<Reserva> reservas = reservaRepository.findVigentesBySalida(salida.getIdSalida());
        for (Reserva reserva : reservas) {
            reservaRepository.save(reserva);
            servicioCorreo.enviarMailReprogramacion(reserva.getCliente().getCorreoElectronico(), reserva.getIdReserva());
        }
    }
}