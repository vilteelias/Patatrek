package com.patatrek.backend.excursion.servicio;

import com.patatrek.backend.excursion.dto.ExcursionRequest;
import com.patatrek.backend.excursion.dto.ExcursionResponse;
import com.patatrek.backend.excursion.modelo.Excursion;
import com.patatrek.backend.excursion.modelo.EstadoExcursion;
import com.patatrek.backend.excursion.repositorio.ExcursionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ExcursionService {
    private final ExcursionRepository excursionRepository;

    public ExcursionService(ExcursionRepository excursionRepository) {
        this.excursionRepository = excursionRepository;
    }

    @Transactional
    public ExcursionResponse registrar(ExcursionRequest req) {
        String nombre = req.nombre().trim();

        if (excursionRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una excursion con el nombre  " + nombre + ".");
        }

        //pasamos los datos del dto a la entidad
        Excursion e = new Excursion();
        e.setNombre(nombre);
        e.setDescripcionGeneral(req.descripcionGeneral());
        e.setDescripcionTecnica(req.descripcionTecnica());
        e.setDuracion(req.duracion());
        e.setRecorridoBase(req.recorridoBase());
        e.setPrecio(req.precio());
        e.setImagenPromocional(req.imagenPromocional());
        e.setDificultad(req.dificultad());
        e.setAceptaMenores(req.aceptaMenores());
        e.setRequiereCertificado(req.requiereCertificado());
        e.setRequiereEquipamiento(req.requiereEquipamiento());
        e.setEstadoExcursion(EstadoExcursion.ACTIVA);

        Excursion guardada = excursionRepository.save(e);
        return ExcursionResponse.de(guardada);
    }

    @Transactional (readOnly = true)
    public List<ExcursionResponse> listar(boolean esAdministrador) {
        List<Excursion> lista = esAdministrador
                ? excursionRepository.findAllByOrderByNombreAsc()
                : excursionRepository.findByEstadoExcursionOrderByNombreAsc(EstadoExcursion.ACTIVA);
        return lista.stream()
                .map(ExcursionResponse::de)
                .toList(); //es una forma moderna de transformar listas
    }
}
