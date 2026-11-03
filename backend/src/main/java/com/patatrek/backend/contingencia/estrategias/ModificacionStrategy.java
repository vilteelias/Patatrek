package com.patatrek.backend.contingencia.estrategias;

import com.patatrek.backend.contingencia.*;
import com.patatrek.backend.salida.Salida;
import com.patatrek.backend.reserva.Reserva;
import com.patatrek.backend.reserva.ReservaRepository;
import com.patatrek.backend.correo.ServicioCorreo;
import org.springframework.stereotype.Component;
import java.util.List;

@Component("MODIFICACION")
public class ModificacionStrategy implements ResolucionContingenciaStrategy {

    private final ReservaRepository reservaRepository;
    private final ServicioCorreo servicioCorreo;

    public ModificacionStrategy(ReservaRepository reservaRepository, ServicioCorreo servicioCorreo) {
        this.reservaRepository = reservaRepository;
        this.servicioCorreo = servicioCorreo;
    }

    @Override
    public void procesarResolucion(Salida salida, Contingencia contingencia, ContingenciaDTO dto) {
        // El recorrido alternativo se guarda en la contingencia según el diagrama de clases
        contingencia.setRecorridoAlternativo(dto.getNuevoRecorrido());
        
        List<Reserva> reservas = reservaRepository.findVigentesBySalida(salida.getIdSalida());
        for (Reserva reserva : reservas) {
            servicioCorreo.enviarMailNuevoItinerario(reserva.getCliente().getCorreoElectronico(), reserva.getIdReserva());
        }
    }
}