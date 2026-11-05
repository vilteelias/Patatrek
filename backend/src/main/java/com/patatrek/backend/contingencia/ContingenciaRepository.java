package com.patatrek.backend.contingencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContingenciaRepository extends JpaRepository<Contingencia, Long> {

    /** La última reprogramación de una salida (para mostrarle al cliente la fecha anterior y el motivo). */
    Optional<Contingencia> findFirstBySalidaIdSalidaAndTipoResolucionOrderByFechaRegistroDesc(Long idSalida,
                                                                                            String tipoResolucion);
}
