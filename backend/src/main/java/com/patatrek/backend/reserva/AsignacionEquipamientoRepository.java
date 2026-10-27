package com.patatrek.backend.reserva;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AsignacionEquipamientoRepository extends JpaRepository<AsignacionEquipamiento, AsignacionEquipamientoId> {

    List<AsignacionEquipamiento> findByReservaIdReservaInAndEstadoAsignacionNot(
            Collection<Long> idsReserva, EstadoAsignacion estadoExcluido);
}
