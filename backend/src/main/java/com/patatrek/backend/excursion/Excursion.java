package com.patatrek.backend.excursion;

import com.patatrek.backend.equipamiento.Equipamiento;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * EXCURSION = id_excursion(PK) + nombre + descripcion_general + descripcion_tecnica + dificultad
 *           + duracion + precio + requiere_certificado + requiere_equipamiento + acepta_menores
 *           + estado_excursion + imagen_promocional + recorrido_base + motivo_baja
 */
@Entity
@Table(name = "excursion")
public class Excursion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idExcursion;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false, length = 1000)
    private String descripcionGeneral;

    @Column(nullable = false, length = 1000)
    private String descripcionTecnica;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Dificultad dificultad;

    /** Duración estimada en días. */
    @Column(nullable = false)
    private Integer duracion;

    /** Precio base de referencia por persona. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private boolean requiereCertificado;

    @Column(nullable = false)
    private boolean requiereEquipamiento;

    @Column(nullable = false)
    private boolean aceptaMenores;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoExcursion estadoExcursion = EstadoExcursion.ACTIVA;

    /** Ruta pública de la imagen, por ejemplo /uploads/excursiones/3-ab12.png */
    @Column(length = 255)
    private String imagenPromocional;

    @Column(nullable = false, length = 1000)
    private String recorridoBase;

    @Column(length = 500)
    private String motivoBaja;

    /** Tabla E/E: equipamiento obligatorio de la excursión (CU-05). */
    @ManyToMany
    @JoinTable(
            name = "excursion_equipamiento",
            joinColumns = @JoinColumn(name = "id_excursion"),
            inverseJoinColumns = @JoinColumn(name = "id_equipamiento"))
    @OrderBy("nombre ASC")
    private Set<Equipamiento> equipamientoObligatorio = new LinkedHashSet<>();

    public boolean estaActiva() {
        return estadoExcursion == EstadoExcursion.ACTIVA;
    }

    // ---------- getters y setters ----------

    public Long getIdExcursion() { return idExcursion; }
    public void setIdExcursion(Long idExcursion) { this.idExcursion = idExcursion; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcionGeneral() { return descripcionGeneral; }
    public void setDescripcionGeneral(String descripcionGeneral) { this.descripcionGeneral = descripcionGeneral; }

    public String getDescripcionTecnica() { return descripcionTecnica; }
    public void setDescripcionTecnica(String descripcionTecnica) { this.descripcionTecnica = descripcionTecnica; }

    public Dificultad getDificultad() { return dificultad; }
    public void setDificultad(Dificultad dificultad) { this.dificultad = dificultad; }

    public Integer getDuracion() { return duracion; }
    public void setDuracion(Integer duracion) { this.duracion = duracion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public boolean isRequiereCertificado() { return requiereCertificado; }
    public void setRequiereCertificado(boolean requiereCertificado) { this.requiereCertificado = requiereCertificado; }

    public boolean isRequiereEquipamiento() { return requiereEquipamiento; }
    public void setRequiereEquipamiento(boolean requiereEquipamiento) { this.requiereEquipamiento = requiereEquipamiento; }

    public boolean isAceptaMenores() { return aceptaMenores; }
    public void setAceptaMenores(boolean aceptaMenores) { this.aceptaMenores = aceptaMenores; }

    public EstadoExcursion getEstadoExcursion() { return estadoExcursion; }
    public void setEstadoExcursion(EstadoExcursion estadoExcursion) { this.estadoExcursion = estadoExcursion; }

    public String getImagenPromocional() { return imagenPromocional; }
    public void setImagenPromocional(String imagenPromocional) { this.imagenPromocional = imagenPromocional; }

    public String getRecorridoBase() { return recorridoBase; }
    public void setRecorridoBase(String recorridoBase) { this.recorridoBase = recorridoBase; }

    public String getMotivoBaja() { return motivoBaja; }
    public void setMotivoBaja(String motivoBaja) { this.motivoBaja = motivoBaja; }

    public Set<Equipamiento> getEquipamientoObligatorio() { return equipamientoObligatorio; }
    public void setEquipamientoObligatorio(Set<Equipamiento> equipamientoObligatorio) { this.equipamientoObligatorio = equipamientoObligatorio; }
}
