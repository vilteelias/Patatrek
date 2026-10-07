package com.patatrek.backend.excursion.modelo;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "excursion")
public class Excursion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_excursion")
    private Long idExcursion;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "descripcion_general", nullable = false, length = 1000)
    private String descripcionGeneral;

    @Column(name = "descripcion_tecnica", nullable = false, length = 2000)
    private String descripcionTecnica;

    @Enumerated(EnumType.STRING)
    @Column(name = "dificultad", nullable = false, length = 20)
    private Dificultad dificultad;

    //duracion en días
    @Column(name = "duracion", nullable = false)
    private Integer duracion;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "requiere_certificado", nullable = false)
    private boolean requiereCertificado;

    @Column(name = "requiere_equipamiento", nullable = false)
    private boolean requiereEquipamiento;

    @Column(name = "acepta_menores", nullable = false)
    private boolean aceptaMenores;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_excursion", nullable = false, length = 20)
    private EstadoExcursion estadoExcursion = EstadoExcursion.ACTIVA;

    @Column(name = "imagen_promocional", length = 500)
    private String imagenPromocional;

    @Column(name = "recorrido_base", nullable = false, length = 1000)
    private String recorridoBase;

    @Column(name = "motivo_baja", length = 500)
    private String motivoBaja;

    //constructor vacío para el JPA
    public Excursion() {}


    //Getters y Setters
    public Long getIdExcursion() {
        return idExcursion;
    }

    public void setIdExcursion(Long idExcursion) {
        this.idExcursion = idExcursion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcionGeneral() {
        return descripcionGeneral;
    }

    public void setDescripcionGeneral(String descripcionGeneral) {
        this.descripcionGeneral = descripcionGeneral;
    }

    public String getDescripcionTecnica() {
        return descripcionTecnica;
    }

    public void setDescripcionTecnica(String descripcionTecnica) {
        this.descripcionTecnica = descripcionTecnica;
    }

    public Dificultad getDificultad() {
        return dificultad;
    }

    public void setDificultad(Dificultad dificultad) {
        this.dificultad = dificultad;
    }

    public Integer getDuracion() {
        return duracion;
    }

    public void setDuracion(Integer duracion) {
        this.duracion = duracion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public boolean isRequiereCertificado() {
        return requiereCertificado;
    }

    public void setRequiereCertificado(boolean requiereCertificado) {
        this.requiereCertificado = requiereCertificado;
    }

    public boolean isRequiereEquipamiento() {
        return requiereEquipamiento;
    }

    public void setRequiereEquipamiento(boolean requiereEquipamiento) {
        this.requiereEquipamiento = requiereEquipamiento;
    }

    public boolean isAceptaMenores() {
        return aceptaMenores;
    }

    public void setAceptaMenores(boolean aceptaMenores) {
        this.aceptaMenores = aceptaMenores;
    }

    public EstadoExcursion getEstadoExcursion() {
        return estadoExcursion;
    }

    public void setEstadoExcursion(EstadoExcursion estadoExcursion) {
        this.estadoExcursion = estadoExcursion;
    }

    public String getImagenPromocional() {
        return imagenPromocional;
    }

    public void setImagenPromocional(String imagenPromocional) {
        this.imagenPromocional = imagenPromocional;
    }

    public String getRecorridoBase() {
        return recorridoBase;
    }

    public void setRecorridoBase(String recorridoBase) {
        this.recorridoBase = recorridoBase;
    }

    public String getMotivoBaja() {
        return motivoBaja;
    }

    public void setMotivoBaja(String motivoBaja) {
        this.motivoBaja = motivoBaja;
    }
}
