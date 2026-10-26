package com.patatrek.backend.reserva;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * PARTICIPANTE = id_participante(PK) + id_reserva(FK) + dni + nombre + apellido + correo_electronico
 *              + nro_telefono + fecha_nacimiento + estado_asistencia + estado_requisitos
 */
@Entity
@Table(name = "participante") // el DNI es único dentro de cada reserva (UQ_participante_reserva_dni en el script)
public class Participante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idParticipante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_reserva", nullable = false)
    private Reserva reserva;

    @Column(nullable = false, length = 15)
    private String dni;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 60)
    private String apellido;

    @Column(nullable = false, length = 120)
    private String correoElectronico;

    @Column(nullable = false, length = 30)
    private String nroTelefono;

    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private EstadoAsistencia estadoAsistencia;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private EstadoRequisitos estadoRequisitos;

    public Participante() {}

    public Participante(Reserva reserva, String dni, String nombre, String apellido, String correoElectronico,
                        String nroTelefono, LocalDate fechaNacimiento) {
        this.reserva = reserva;
        this.dni = dni;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correoElectronico = correoElectronico;
        this.nroTelefono = nroTelefono;
        this.fechaNacimiento = fechaNacimiento;
    }

    /** "Apellido, Nombre" */
    public String getNombreCompleto() {
        return apellido + ", " + nombre;
    }

    public Long getIdParticipante() { return idParticipante; }
    public void setIdParticipante(Long idParticipante) { this.idParticipante = idParticipante; }

    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getCorreoElectronico() { return correoElectronico; }
    public void setCorreoElectronico(String correoElectronico) { this.correoElectronico = correoElectronico; }

    public String getNroTelefono() { return nroTelefono; }
    public void setNroTelefono(String nroTelefono) { this.nroTelefono = nroTelefono; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public EstadoAsistencia getEstadoAsistencia() { return estadoAsistencia; }
    public void setEstadoAsistencia(EstadoAsistencia estadoAsistencia) { this.estadoAsistencia = estadoAsistencia; }

    public EstadoRequisitos getEstadoRequisitos() { return estadoRequisitos; }
    public void setEstadoRequisitos(EstadoRequisitos estadoRequisitos) { this.estadoRequisitos = estadoRequisitos; }
}
