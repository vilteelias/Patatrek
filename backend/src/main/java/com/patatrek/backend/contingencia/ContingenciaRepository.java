package com.patatrek.backend.contingencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContingenciaRepository extends JpaRepository<Contingencia, Long> {
}
