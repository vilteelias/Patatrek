package com.patatrek.backend.reserva;

import com.patatrek.backend.salida.Salida;
import com.patatrek.backend.usuario.Cliente;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RESERVA = id_reserva(PK) + id_salida(FK) + id_cliente(FK) + fecha_hora + estado_reserva + monto_total
 *         + saldo_pendiente + modalidad_alojamiento + motivo_cancelacion + reprogramacion_pendiente
 *         + cantidad_participantes (NUEVO) + fecha_hora_vencimiento_retencion (NUEVO)
 *
 * Por ahora solo se usa para armar la nómina de la salida. El proceso de reserva (CU-18) llega en el módulo Reservas.
 */
@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_salida", nullable = false)
    private Salida salida;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estadoReserva;

    /** NUEVO: cupos que la reserva retiene u ocupa en la salida. */
    @Column(nullable = false)
    private int cantidadParticipantes;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoPendiente;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private ModalidadAlojamiento modalidadAlojamiento;

    /** NUEVO: fecha_hora + 10 minutos mientras está "Pendiente de pago"; null al consolidar. */
    private LocalDateTime fechaHoraVencimientoRetencion;

    @Column(length = 500)
    private String motivoCancelacion;

    @Column(nullable = false)
    private boolean reprogramacionPendiente;

    public Long getIdReserva() { return idReserva; }
    public void setIdReserva(Long idReserva) { this.idReserva = idReserva; }

    public Salida getSalida() { return salida; }
    public void setSalida(Salida salida) { this.salida = salida; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public EstadoReserva getEstadoReserva() { return estadoReserva; }
    public void setEstadoReserva(EstadoReserva estadoReserva) { this.estadoReserva = estadoReserva; }

    public int getCantidadParticipantes() { return cantidadParticipantes; }
    public void setCantidadParticipantes(int cantidadParticipantes) { this.cantidadParticipantes = cantidadParticipantes; }

    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }

    public BigDecimal getSaldoPendiente() { return saldoPendiente; }
    public void setSaldoPendiente(BigDecimal saldoPendiente) { this.saldoPendiente = saldoPendiente; }

    public ModalidadAlojamiento getModalidadAlojamiento() { return modalidadAlojamiento; }
    public void setModalidadAlojamiento(ModalidadAlojamiento modalidadAlojamiento) { this.modalidadAlojamiento = modalidadAlojamiento; }

    public LocalDateTime getFechaHoraVencimientoRetencion() { return fechaHoraVencimientoRetencion; }
    public void setFechaHoraVencimientoRetencion(LocalDateTime fechaHoraVencimientoRetencion) { this.fechaHoraVencimientoRetencion = fechaHoraVencimientoRetencion; }

    public String getMotivoCancelacion() { return motivoCancelacion; }
    public void setMotivoCancelacion(String motivoCancelacion) { this.motivoCancelacion = motivoCancelacion; }

    public boolean isReprogramacionPendiente() { return reprogramacionPendiente; }
    public void setReprogramacionPendiente(boolean reprogramacionPendiente) { this.reprogramacionPendiente = reprogramacionPendiente; }
}
