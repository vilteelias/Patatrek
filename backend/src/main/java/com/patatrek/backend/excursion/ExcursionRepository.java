package com.patatrek.backend.excursion;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExcursionRepository extends JpaRepository<Excursion, Long> {

    @EntityGraph(attributePaths = "equipamientoObligatorio")
    List<Excursion> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = "equipamientoObligatorio")
    List<Excursion> findByEstadoExcursionOrderByNombreAsc(EstadoExcursion estado);

    @EntityGraph(attributePaths = "equipamientoObligatorio")
    Optional<Excursion> findWithEquipamientoByIdExcursion(Long idExcursion);

    /** El nombre es único entre activas e inactivas (regla de negocio CU-04). */
    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdExcursionNot(String nombre, Long idExcursion);
}
