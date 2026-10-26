package com.patatrek.backend.salida;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** REFUGIO = id_refugio(PK) + nombre + capacidad_plazas + precio_noche */
@Entity
@Table(name = "refugio")
public class Refugio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRefugio;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false)
    private int capacidadPlazas;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioNoche;

    public Refugio() {}

    public Refugio(String nombre, int capacidadPlazas, BigDecimal precioNoche) {
        this.nombre = nombre;
        this.capacidadPlazas = capacidadPlazas;
        this.precioNoche = precioNoche;
    }

    public Long getIdRefugio() { return idRefugio; }
    public void setIdRefugio(Long idRefugio) { this.idRefugio = idRefugio; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getCapacidadPlazas() { return capacidadPlazas; }
    public void setCapacidadPlazas(int capacidadPlazas) { this.capacidadPlazas = capacidadPlazas; }

    public BigDecimal getPrecioNoche() { return precioNoche; }
    public void setPrecioNoche(BigDecimal precioNoche) { this.precioNoche = precioNoche; }
}
