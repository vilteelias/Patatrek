package com.patatrek.backend.salida;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AsignacionGuiaRepository extends JpaRepository<AsignacionGuia, AsignacionGuiaId> {

    /** Salidas en las que está asignado un guía (CU-10, vista del guía). */
    @EntityGraph(attributePaths = {"salida", "salida.excursion"})
    List<AsignacionGuia> findByGuiaIdUsuario(Long idGuia);

    @EntityGraph(attributePaths = {"salida", "salida.excursion"})
    Optional<AsignacionGuia> findByGuiaIdUsuarioAndSalidaIdSalida(Long idGuia, Long idSalida);

    /** Todos los guías (responsable y ayudantes) de varias salidas. */
    @EntityGraph(attributePaths = "guia")
    List<AsignacionGuia> findBySalidaIdSalidaIn(Collection<Long> idsSalida);
}
