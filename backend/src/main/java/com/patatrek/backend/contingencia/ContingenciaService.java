package com.patatrek.backend.contingencia;

import com.patatrek.backend.salida.Salida;
import com.patatrek.backend.salida.SalidaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class ContingenciaService {

    // Spring inyecta automáticamente las 3 estrategias acá
    private final Map<String, ResolucionContingenciaStrategy> estrategias;
    private final SalidaRepository salidaRepository;
    private final ContingenciaRepository contingenciaRepository;

    public ContingenciaService(Map<String, ResolucionContingenciaStrategy> estrategias, 
                               SalidaRepository salidaRepository, 
                               ContingenciaRepository contingenciaRepository) {
        this.estrategias = estrategias;
        this.salidaRepository = salidaRepository;
        this.contingenciaRepository = contingenciaRepository;
    }

    @Transactional
    public void registrarContingencia(Long idSalida, ContingenciaDTO dto) {
        Salida salida = salidaRepository.findById(idSalida)
                .orElseThrow(() -> new RuntimeException("Salida no encontrada con ID: " + idSalida));

        // 1. Guardar la entidad Contingencia base
        Contingencia contingencia = new Contingencia();
        contingencia.setTipoContingencia(dto.getTipoContingencia());
        contingencia.setMotivo(dto.getMotivo());
        contingencia.setTipoResolucion(dto.getResolucion());
        contingencia.setSalida(salida);
        contingenciaRepository.save(contingencia);

        // 2. Buscar la estrategia según lo que haya mandado el frontend (ej: "CANCELACION")
        String claveEstrategia = dto.getResolucion().toUpperCase();
        ResolucionContingenciaStrategy estrategia = estrategias.get(claveEstrategia);

        if (estrategia == null) {
            throw new IllegalArgumentException("Tipo de resolución de contingencia no soportado: " + claveEstrategia);
        }

        // 3. Ejecutar la lógica específica
        estrategia.procesarResolucion(salida, contingencia, dto);

        // 4. Guardar los cambios finales en la salida
        salidaRepository.save(salida);
    }
}
