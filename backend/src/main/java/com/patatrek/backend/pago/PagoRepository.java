package com.patatrek.backend.pago;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByReservaIdReservaOrderByIdPagoAsc(Long idReserva);

    List<Pago> findByReservaIdReservaIn(Collection<Long> idsReserva);
}
