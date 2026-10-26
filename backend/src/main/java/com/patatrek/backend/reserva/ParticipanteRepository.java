package com.patatrek.backend.reserva;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ParticipanteRepository extends JpaRepository<Participante, Long> {

    /** Nómina de una salida: participantes de las reservas en los estados indicados (CU-22). */
    @EntityGraph(attributePaths = "reserva")
    List<Participante> findByReservaSalidaIdSalidaAndReservaEstadoReservaInOrderByApellidoAscNombreAsc(
            Long idSalida, Collection<EstadoReserva> estados);
}
