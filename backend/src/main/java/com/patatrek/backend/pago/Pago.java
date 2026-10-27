package com.patatrek.backend.pago;

import com.patatrek.backend.reserva.Reserva;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PAGO = id_pago(PK) + id_reserva(FK) + fecha_pago + importe + nro_transaccion + modo_pago + estado_pago
 *
 * CORRECCIÓN DE LA CÁTEDRA: el pago NO tiene ningún dato de la tarjeta (número, vencimiento, CVV, titular).
 * Esos datos se ingresan solamente en la pasarela de pagos; el sistema guarda únicamente el resultado:
 * el número de transacción que devuelve la pasarela, el importe, el modo y el estado.
 */
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_reserva", nullable = false)
    private Reserva reserva;

    /** Momento en que se registró el pago; se actualiza cuando la pasarela responde. */
    @Column(nullable = false)
    private LocalDateTime fechaPago;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    /** Identificador de la operación que devuelve la pasarela. */
    @Column(length = 100)
    private String nroTransaccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ModoPago modoPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoPago estadoPago = EstadoPago.PENDIENTE;

    protected Pago() {}

    public Pago(Reserva reserva, BigDecimal importe, ModoPago modoPago) {
        this.reserva = reserva;
        this.importe = importe;
        this.modoPago = modoPago;
        this.fechaPago = LocalDateTime.now();
        this.estadoPago = EstadoPago.PENDIENTE;
    }

    public Long getIdPago() { return idPago; }
    public Reserva getReserva() { return reserva; }

    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }

    public BigDecimal getImporte() { return importe; }

    public String getNroTransaccion() { return nroTransaccion; }
    public void setNroTransaccion(String nroTransaccion) { this.nroTransaccion = nroTransaccion; }

    public ModoPago getModoPago() { return modoPago; }

    public EstadoPago getEstadoPago() { return estadoPago; }
    public void setEstadoPago(EstadoPago estadoPago) { this.estadoPago = estadoPago; }
}
