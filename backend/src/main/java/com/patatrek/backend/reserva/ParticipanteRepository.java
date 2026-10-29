package com.patatrek.backend.reserva;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParticipanteRepository extends JpaRepository<Participante, Long> {

    /** Nómina de una salida: participantes de las reservas en los estados indicados (CU-22). */
    @EntityGraph(attributePaths = "reserva")
    List<Participante> findByReservaSalidaIdSalidaAndReservaEstadoReservaInOrderByApellidoAscNombreAsc(
            Long idSalida, Collection<EstadoReserva> estados);

    /** Participantes de una reserva (CU-24: certificados del Cliente). */
    List<Participante> findByReservaIdReservaOrderByApellidoAscNombreAsc(Long idReserva);

    /** Acceso del Participante: ID de reserva + DNI (CU-19, CU-24). El DNI es único dentro de cada reserva. */
    Optional<Participante> findByReservaIdReservaAndDni(Long idReserva, String dni);
}
