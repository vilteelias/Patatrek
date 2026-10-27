package com.patatrek.backend.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuiaRepository extends JpaRepository<Guia, Long> {

    Optional<Guia> findByCorreoElectronicoIgnoreCase(String correoElectronico);

    Optional<Guia> findFirstByOrderByIdUsuarioAsc();
}
