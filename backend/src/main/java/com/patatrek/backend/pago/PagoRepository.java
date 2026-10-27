package com.patatrek.backend.pago;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByReservaIdReservaOrderByIdPagoAsc(Long idReserva);
}
