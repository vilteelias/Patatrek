package com.patatrek.backend.salida;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AsignacionRefugioRepository extends JpaRepository<AsignacionRefugio, AsignacionRefugioId> {

    @EntityGraph(attributePaths = "refugio")
    List<AsignacionRefugio> findBySalidaIdSalidaInOrderByOrdenVisitaAsc(Collection<Long> idsSalida);
}
