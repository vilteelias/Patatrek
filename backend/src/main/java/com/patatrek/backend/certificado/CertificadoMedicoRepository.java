package com.patatrek.backend.certificado;

import com.patatrek.backend.reserva.EstadoReserva;
import com.patatrek.backend.salida.EstadoSalida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/*JpaRepository ya tiene metodos bases
Los (CreateReadUpdateDelete) ya son brindados por JpaRepository (para esta entidad/clase CertificadoMedico)
se declaran (sin impelmentacion) cualquiera que no sean CRUD basicos, a traves de Query methods o Custom Query (SQL)
 */
public interface CertificadoMedicoRepository extends JpaRepository<CertificadoMedico, Long> {

    CertificadoMedico findByIdCertificadoMedico(Long idCertificado);

    List<CertificadoMedico> findByEstadoCertificado(EstadoCertificado estado);

    boolean existsByParticipanteIdParticipante(Long idparticipante);

    List<CertificadoMedico> findAllByOrderByFechaSubidaAsc();

    /** Certificados de varios participantes, del más reciente al más antiguo. */
    List<CertificadoMedico> findByParticipanteIdParticipanteInOrderByFechaSubidaDesc(Collection<Long> idsParticipante);

    /** NUEVO (CU-24): certificados que ya existen para un conjunto de participantes. */
    List<CertificadoMedico> findByParticipanteIdParticipanteIn(Collection<Long> idsParticipante);

    /**
     * NUEVO (CU-25): certificados a validar. Solo los "Cargado", de reservas Señadas/Confirmadas y de salidas
     * que todavía no iniciaron, con la salida más próxima primero. Los "join fetch" traen participante, reserva,
     * salida y excursión en una sola consulta (la tabla del administrador usa todos esos datos).
     */
    @Query("""
            select c from CertificadoMedico c
              join fetch c.participante p
              join fetch p.reserva r
              join fetch r.salida s
              join fetch s.excursion
            where c.estadoCertificado = :estado
              and r.estadoReserva in :estadosReserva
              and s.estadoSalida in :estadosSalida
            order by s.fechaInicio asc, s.horaInicio asc, c.fechaSubida asc
            """)
    List<CertificadoMedico> buscarParaValidar(@Param("estado") EstadoCertificado estado,
                                              @Param("estadosReserva") Collection<EstadoReserva> estadosReserva,
                                              @Param("estadosSalida") Collection<EstadoSalida> estadosSalida);
}
