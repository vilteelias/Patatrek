package com.patatrek.backend.excursion.repositorio;

import com.patatrek.backend.excursion.modelo.Excursion;
import com.patatrek.backend.excursion.modelo.EstadoExcursion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExcursionRepository extends JpaRepository<Excursion, Long>{

    //el nombre de la excursion debe ser único
    boolean existsByNombreIgnoreCase(String nombre);

    //el visitante-cliente solo ve las excursiones activas
    List<Excursion> findByEstadoExcursionOrderByNombreAsc(EstadoExcursion estado);

    //el administrador ve todas las excursiones
    List<Excursion> findAllByOrderByNombreAsc();
}
