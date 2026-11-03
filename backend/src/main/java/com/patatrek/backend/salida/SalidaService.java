package com.patatrek.backend.salida;

import com.patatrek.backend.excursion.Excursion;
import com.patatrek.backend.excursion.ExcursionRepository;
import com.patatrek.backend.usuario.Guia;
import com.patatrek.backend.usuario.GuiaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Iterator;
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

    @Transactional(readOnly = true)
public List<Salida> obtenerTodas() {
    return salidaRepository.findAll();
}

    @Transactional(readOnly = true)
    public Salida obtenerPorId(Long id) {
        return salidaRepository.findByIdConGuias(id)
                .orElseThrow(() -> new RuntimeException("Salida no encontrada con ID: " + id));
    }

    @Transactional
    public Salida asignarAyudantesYRecalcularCupos(Long idSalida, AsignacionAyudantesDTO dto) {
        List<Long> idsGuiaAyudantes = dto.getIdsGuiaAyudantes();

        Salida salida = salidaRepository.findByIdConGuias(idSalida)
                .orElseThrow(() -> new RuntimeException("Salida no encontrada con ID: " + idSalida));

        // 1. Identificar si algún ayudante coincide con el guía responsable de la salida
        for (AsignacionGuia ag : salida.getAsignacionGuias()) {
            if (ag.getTipoGuia() == TipoGuia.RESPONSABLE && ag.getGuia() != null) {
                Long idResponsable = ag.getGuia().getIdUsuario(); // Ajustá si tu ID se llama diferente
                if (idsGuiaAyudantes.contains(idResponsable)) {
                    throw new RuntimeException("El guía responsable de la salida no puede ser asignado también como ayudante.");
                }
            }
        }

        // 2. Remover guías ayudantes anteriores de forma segura
        Iterator<AsignacionGuia> iterator = salida.getAsignacionGuias().iterator();
        while (iterator.hasNext()) {
            AsignacionGuia ag = iterator.next();
            if (ag.getTipoGuia() == TipoGuia.AYUDANTE) {
                iterator.remove();
            }
        }

        // 3. Agregar los nuevos ayudantes
        for (Long idGuia : idsGuiaAyudantes) {
            Guia guia = guiaRepository.findById(idGuia)
                    .orElseThrow(() -> new RuntimeException("Guía no encontrado con ID: " + idGuia));
            
            AsignacionGuia nuevaAsignacion = new AsignacionGuia(guia, salida, TipoGuia.AYUDANTE);
            salida.getAsignacionGuias().add(nuevaAsignacion);
        }

        // 4. Recálculo de cupos
        int cupoBase = salida.getCupoBase();
        int cantidadAyudantes = idsGuiaAyudantes.size();
        int variacion = (int) Math.floor(cupoBase * 0.25 * cantidadAyudantes);
        salida.setCupoTotal(cupoBase + variacion);

        return salidaRepository.save(salida);
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
