package com.patatrek.backend.certificado;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CertificadoMedicoRepository extends JpaRepository<CertificadoMedico, Long> {

    /** Certificados de varios participantes, del más reciente al más antiguo. */
    List<CertificadoMedico> findByParticipanteIdParticipanteInOrderByFechaSubidaDesc(Collection<Long> idsParticipante);
}
