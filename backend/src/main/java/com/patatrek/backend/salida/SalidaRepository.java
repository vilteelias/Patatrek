package com.patatrek.backend.salida;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface SalidaRepository extends JpaRepository<Salida, Long> {

    boolean existsByExcursionIdExcursionAndEstadoSalidaIn(Long idExcursion, Collection<EstadoSalida> estados);

    /** IDs de las excursiones que tienen al menos una salida en alguno de los estados dados. */
    @Query("select distinct s.excursion.idExcursion from Salida s where s.estadoSalida in :estados")
    Set<Long> findIdsExcursionConSalidasEn(@Param("estados") Collection<EstadoSalida> estados);

    @Query("select s.idSalida from Salida s where s.excursion.nombre = :nombre and s.estadoSalida = :estado order by s.idSalida")
    List<Long> findIdsPorExcursionYEstado(@Param("nombre") String nombreExcursion, @Param("estado") EstadoSalida estado);
}
