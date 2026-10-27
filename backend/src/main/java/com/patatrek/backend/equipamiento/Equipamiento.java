package com.patatrek.backend.equipamiento;

import jakarta.persistence.*;

/**
 * EQUIPAMIENTO = id_equipamiento(PK) + nombre + descripcion + cantidad_stock + stock_retenido
 * Catálogo precargado del que el administrador elige el equipamiento obligatorio (CU-05).
 * stock_retenido: unidades apartadas por reservas "Pendiente de Pago" (retención de 10 minutos, RF-15).
 */
@Entity
@Table(name = "equipamiento")
public class Equipamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEquipamiento;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private int cantidadStock;

    @Column(nullable = false)
    private int stockRetenido;

    public Equipamiento() {}

    public Equipamiento(String nombre, String descripcion, int cantidadStock) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.cantidadStock = cantidadStock;
    }

    public Long getIdEquipamiento() { return idEquipamiento; }
    public void setIdEquipamiento(Long idEquipamiento) { this.idEquipamiento = idEquipamiento; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getCantidadStock() { return cantidadStock; }
    public void setCantidadStock(int cantidadStock) { this.cantidadStock = cantidadStock; }

    public int getStockRetenido() { return stockRetenido; }
    public void setStockRetenido(int stockRetenido) { this.stockRetenido = stockRetenido; }
}
