package com.patatrek.backend.equipamiento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipamientoRepository extends JpaRepository<Equipamiento, Long> {

    List<Equipamiento> findAllByOrderByNombreAsc();
}
