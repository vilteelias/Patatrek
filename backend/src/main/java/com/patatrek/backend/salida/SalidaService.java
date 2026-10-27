package com.patatrek.backend.salida;

import com.patatrek.backend.excursion.Excursion;
import com.patatrek.backend.excursion.ExcursionRepository;
import com.patatrek.backend.usuario.Guia;
import com.patatrek.backend.usuario.GuiaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SalidaService {

    @Autowired
    private SalidaRepository salidaRepository;

    @Autowired
    private ExcursionRepository excursionRepository;

    @Autowired
    private GuiaRepository guiaRepository;

    @Autowired
    private RefugioRepository refugioRepository;

    @Autowired
    private AsignacionRefugioRepository asignacionRefugioRepository;

    public List<Salida> obtenerTodas() {
        return salidaRepository.findAll();
    }

    @Transactional
    public Salida planificarSalida(PlanificarSalidaDTO dto) {
        Excursion excursion = excursionRepository.findById(dto.getIdExcursion())
                .orElseThrow(() -> new RuntimeException("Excursión no encontrada con ID: " + dto.getIdExcursion()));

        Guia guiaResponsable = guiaRepository.findById(dto.getIdGuiaResponsable())
                .orElseThrow(() -> new RuntimeException("Guía no encontrado con ID: " + dto.getIdGuiaResponsable()));

        Salida salida = new Salida();
        salida.setExcursion(excursion);
        salida.setFechaInicio(dto.getFechaInicio());
        salida.setHoraInicio(dto.getHoraInicio());
        salida.setFechaFinalizacion(dto.getFechaFinalizacion());
        salida.setHoraFinalizacion(dto.getHoraFinalizacion());
        salida.setCupoBase(guiaResponsable.getCapacidadEstandar());
        salida.setCupoMaximo(dto.getCupoMaximo());
        salida.setEstadoSalida(EstadoSalida.DISPONIBLE);

        Salida salidaGuardada = salidaRepository.save(salida);

        // Registrar asignación del guía responsable
        AsignacionGuia asignacionGuia = new AsignacionGuia(guiaResponsable, salidaGuardada, TipoGuia.RESPONSABLE);
        
        salidaGuardada.addAsignacionGuia(asignacionGuia);
        salidaRepository.save(salidaGuardada);
        

        // 2. Registrar asignación de refugios por cada noche (si aplica por ser multidía)
        if (dto.getRefugiosNoches() != null && !dto.getRefugiosNoches().isEmpty()) {
            for (NocheRefugioDTO nocheDto : dto.getRefugiosNoches()) {
                Refugio refugio = refugioRepository.findById(nocheDto.getIdRefugio())
                        .orElseThrow(() -> new RuntimeException("Refugio no encontrado con ID: " + nocheDto.getIdRefugio()));

                // Usar el constructor parametrizado que maneja la clave compuesta
                AsignacionRefugio asignacionRefugio = new AsignacionRefugio(
                        refugio,
                        salidaGuardada,
                        nocheDto.getOrdenVisita(),
                        nocheDto.getPlazasReservadas()
                );

                asignacionRefugioRepository.save(asignacionRefugio);
            }
        }

        return salidaGuardada;
    }
}
