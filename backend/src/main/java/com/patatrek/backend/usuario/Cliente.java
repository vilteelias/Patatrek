package com.patatrek.backend.usuario;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * CLIENTE = id_usuario(PK) + dni + nombre + apellido + correo_electronico + contrasenia + telefono + fecha_nacimiento
 * Por ahora solo se usa como titular de las reservas; el registro (CU-01) llega con Autenticación.
 */
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @Column(nullable = false, unique = true, length = 15)
    private String dni;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 60)
    private String apellido;

    @Column(nullable = false, unique = true, length = 120)
    private String correoElectronico;

    @Column(nullable = false, length = 255)
    private String contrasenia;

    @Column(nullable = false, length = 30)
    private String telefono;

    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    public Cliente() {}

    public Cliente(String dni, String nombre, String apellido, String correoElectronico, String telefono,
                   LocalDate fechaNacimiento) {
        this.dni = dni;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correoElectronico = correoElectronico;
        this.telefono = telefono;
        this.fechaNacimiento = fechaNacimiento;
        this.contrasenia = "SIN_DEFINIR"; // se define en el módulo de Autenticación
    }

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getCorreoElectronico() { return correoElectronico; }
    public void setCorreoElectronico(String correoElectronico) { this.correoElectronico = correoElectronico; }

    public String getContrasenia() { return contrasenia; }
    public void setContrasenia(String contrasenia) { this.contrasenia = contrasenia; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
}
