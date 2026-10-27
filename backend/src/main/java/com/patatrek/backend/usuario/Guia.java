package com.patatrek.backend.usuario;

import jakarta.persistence.*;

/**
 * GUIA = id_usuario(PK) + dni + nombre + apellido + correo_electronico + contraseña + telefono + capacidad_estandar
 */
@Entity
@Table(name = "guia")
public class Guia {

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

    /** Hash de la contraseña (se usa en el módulo de Autenticación). */
    @Column(nullable = false, length = 255)
    private String contrasenia;

    @Column(nullable = false, length = 30)
    private String telefono;

    /** Cantidad de participantes que puede guiar; se usa como cupo base de la salida (RF-08). */
    @Column(nullable = false)
    private int capacidadEstandar;

    public Guia() {}

    public Guia(String dni, String nombre, String apellido, String correoElectronico, String telefono, int capacidadEstandar) {
        this.dni = dni;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correoElectronico = correoElectronico;
        this.telefono = telefono;
        this.capacidadEstandar = capacidadEstandar;
        this.contrasenia = "SIN_DEFINIR"; // se define en el módulo de Autenticación
    }

    /** "Apellido, Nombre" */
    public String getNombreCompleto() {
        return apellido + ", " + nombre;
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

    public int getCapacidadEstandar() { return capacidadEstandar; }
    public void setCapacidadEstandar(int capacidadEstandar) { this.capacidadEstandar = capacidadEstandar; }
}
