package com.patatrek.backend.usuario;

import jakarta.persistence.*;

/**
 * ADMINISTRADOR = id_usuario(PK) + dni + nombre + apellido + correo_electronico + contrasenia + telefono
 */
@Entity
@Table(name = "administrador")
public class Administrador {

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

    /** Hash BCrypt de la contraseña (nunca texto plano). */
    @Column(nullable = false, length = 255)
    private String contrasenia;

    @Column(nullable = false, length = 30)
    private String telefono;

    protected Administrador() {}

    public Administrador(String dni, String nombre, String apellido, String correoElectronico, String contrasenia,
                         String telefono) {
        this.dni = dni;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correoElectronico = correoElectronico;
        this.contrasenia = contrasenia;
        this.telefono = telefono;
    }

    public Long getIdUsuario() { return idUsuario; }
    public String getDni() { return dni; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getCorreoElectronico() { return correoElectronico; }
    public String getContrasenia() { return contrasenia; }
    public void setContrasenia(String contrasenia) { this.contrasenia = contrasenia; }
    public String getTelefono() { return telefono; }
}
