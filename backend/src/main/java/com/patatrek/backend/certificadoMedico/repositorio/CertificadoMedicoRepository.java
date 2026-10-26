package com.patatrek.backend.certificadoMedico.repositorio;

import com.patatrek.backend.certificadoMedico.modelo.CertificadoMedico;
import com.patatrek.backend.certificadoMedico.modelo.EstadoCertificado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*JpaRepository ya tiene metodos bases
Los (CreateReadUpdateDelete) ya son brindados por JpaRepository (para esta entidad/clase CertificadoMedico)
se declaran (sin impelmentacion) cualquiera que no sean CRUD basicos, a traves de Query methods o Custom Query (SQL)
 */
public interface CertificadoMedicoRepository extends JpaRepository<CertificadoMedico, Long>{

    CertificadoMedico findByIdCertificadoMedico(Long idCertificado);

    List<CertificadoMedico> findByEstadoCertificado(EstadoCertificado estado);

    boolean existsByParticipanteIdParticipante(Long idparticipante);

    /* obtener certificados a validar (con Custom Query JPQL)
        pendiente a colocar:
        salida: completa/disponible
        reserva: confirmada/señada
     */

    List<CertificadoMedico> findAllByOrderByFechaSubidaAsc ();



}
