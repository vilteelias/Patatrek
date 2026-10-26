package com.patatrek.backend.salida.servicio;

import com.patatrek.backend.salida.dto.PlanificarSalidaDTO;
import com.patatrek.backend.salida.modelo.Salida;
import com.patatrek.backend.salida.repositorio.SalidaRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalidaServicio {

    @Autowired
    private SalidaRepositorio salidaRepositorio;

    public List<Salida> obtenerTodas() {
        return salidaRepositorio.findAll();
    }

    public Salida planificarSalida(PlanificarSalidaDTO dto) {
        Salida nuevaSalida = new Salida();
        nuevaSalida.setIdExcursion(dto.getIdExcursion());
        nuevaSalida.setFechaInicio(dto.getFechaInicio());
        nuevaSalida.setHoraInicio(dto.getHoraInicio());
        nuevaSalida.setFechaFin(dto.getFechaFin());
        nuevaSalida.setHoraFin(dto.getHoraFin());
        nuevaSalida.setGuiaResponsable(dto.getGuiaResponsable());
        nuevaSalida.setCupoMaximo(dto.getCupoMaximo());
        nuevaSalida.setCupoDisponible(dto.getCupoMaximo()); // Inicia con cupo total disponible
        nuevaSalida.setEstado("DISPONIBLE");
        nuevaSalida.setRefugio(dto.getRefugio());
        nuevaSalida.setCapacidadRefugio(dto.getCapacidadRefugio());

        return salidaRepositorio.save(nuevaSalida);
    }
}