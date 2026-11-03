package com.patatrek.backend.reserva;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findVigentesBySalida(Long idSalida);
}
