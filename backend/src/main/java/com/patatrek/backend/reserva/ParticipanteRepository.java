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

    List<Participante> findByReservaIdReservaOrderByIdParticipanteAsc(Long idReserva);

    /** CU-28: el participante se identifica con el ID de reserva y su DNI. */
    @EntityGraph(attributePaths = {"reserva", "reserva.salida", "reserva.salida.excursion"})
    Optional<Participante> findByReservaIdReservaAndDni(Long idReserva, String dni);

    /** CU-28 (panel del cliente): participaciones de una persona, por DNI, en salidas de un estado dado. */
    @EntityGraph(attributePaths = {"reserva", "reserva.salida", "reserva.salida.excursion"})
    List<Participante> findByDniAndReservaSalidaEstadoSalida(String dni, com.patatrek.backend.salida.EstadoSalida estado);

    /** Participantes de una reserva (CU-24: certificados del Cliente). */
    List<Participante> findByReservaIdReservaOrderByApellidoAscNombreAsc(Long idReserva);

}
