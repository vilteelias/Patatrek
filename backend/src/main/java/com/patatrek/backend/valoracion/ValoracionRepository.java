package com.patatrek.backend.valoracion;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {

    boolean existsByParticipanteIdParticipante(Long idParticipante);

    Optional<Valoracion> findByParticipanteIdParticipante(Long idParticipante);

    List<Valoracion> findByParticipanteIdParticipanteIn(Collection<Long> idsParticipante);

    /** Todas las valoraciones con su participante, salida y excursión (CU-29). */
    @EntityGraph(attributePaths = {"participante", "participante.reserva", "participante.reserva.salida",
            "participante.reserva.salida.excursion"})
    List<Valoracion> findAllByOrderByFechaDesc();
}
