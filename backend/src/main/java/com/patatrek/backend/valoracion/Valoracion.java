package com.patatrek.backend.valoracion;

import com.patatrek.backend.reserva.Participante;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * VALORACION = id_valoracion(PK) + id_participante(FK, único) + fecha + calificacion + comentario
 * Una sola valoración por participante: como cada participante pertenece a una reserva de una salida,
 * equivale a "una valoración por persona y por salida realizada" (RF-25).
 */
@Entity
@Table(name = "valoracion")
public class Valoracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idValoracion;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_participante", nullable = false, unique = true)
    private Participante participante;

    @Column(nullable = false)
    private LocalDateTime fecha;

    /** Entero de 1 a 5 (obligatorio). */
    @Column(nullable = false)
    private int calificacion;

    /** Opcional. */
    @Column(length = 1000)
    private String comentario;

    protected Valoracion() {}

    public Valoracion(Participante participante, int calificacion, String comentario, LocalDateTime fecha) {
        this.participante = participante;
        this.calificacion = calificacion;
        this.comentario = comentario;
        this.fecha = fecha;
    }

    public Long getIdValoracion() { return idValoracion; }
    public Participante getParticipante() { return participante; }
    public LocalDateTime getFecha() { return fecha; }
    public int getCalificacion() { return calificacion; }
    public String getComentario() { return comentario; }
}
