package com.patatrek.backend.salida.repositorio;

import com.patatrek.backend.salida.modelo.Salida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalidaRepositorio extends JpaRepository<Salida, Long> {
}