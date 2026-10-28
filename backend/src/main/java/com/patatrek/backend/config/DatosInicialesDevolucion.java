package com.patatrek.backend.config;

import com.patatrek.backend.excursion.Excursion;
import com.patatrek.backend.excursion.ExcursionRepository;
import com.patatrek.backend.pago.EstadoPago;
import com.patatrek.backend.pago.ModoPago;
import com.patatrek.backend.pago.Pago;
import com.patatrek.backend.pago.PagoRepository;
import com.patatrek.backend.reserva.*;
import com.patatrek.backend.salida.EstadoSalida;
import com.patatrek.backend.salida.Salida;
import com.patatrek.backend.salida.SalidaRepository;
import com.patatrek.backend.usuario.Cliente;
import com.patatrek.backend.usuario.ClienteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Datos de ejemplo para CU-27 Registrar devolución de reserva.
 * Las reservas "Pendiente de devolución" las genera la contingencia de salida (CU-14, parte de Salidas);
 * mientras tanto, para poder mostrar la devolución se cargan:
 *  - Una salida del Ascenso Cerro Perito Moreno CANCELADA por contingencia climática, con
 *      · la reserva señada de Julián Ferreyra (2 personas, pagó el 50 %) → pendiente;
 *      · la reserva pagada de Melina Cabral (1 persona, pagó el total) → pendiente;
 *      · la reserva de Paula Medina, ya devuelta hace 2 días            → historial.
 *  - Una salida del Sendero Laguna del Bosque reprogramada por contingencia operativa, con
 *      · la reserva de Diego Sosa, que rechazó la nueva fecha            → pendiente.
 * Se carga una sola vez: si ya hay reservas pendientes de devolución o devueltas, no hace nada.
 */
@Component
@Order(5)
@ConditionalOnProperty(name = "patatrek.cargar-datos-demo", havingValue = "true", matchIfMissing = true)
public class DatosInicialesDevolucion implements CommandLineRunner {

    static final String MOTIVO_CLIMATICA =
            "Cancelación de la salida por contingencia climática: alerta por vientos fuertes en la zona de cumbre.";
    static final String MOTIVO_REPROGRAMACION =
            "Rechazo de la reprogramación de la salida por contingencia operativa (cambio de fecha).";

    private final ClienteRepository clienteRepository;
    private final ExcursionRepository excursionRepository;
    private final SalidaRepository salidaRepository;
    private final ReservaRepository reservaRepository;
    private final ParticipanteRepository participanteRepository;
    private final PagoRepository pagoRepository;

    public DatosInicialesDevolucion(ClienteRepository clienteRepository, ExcursionRepository excursionRepository,
                                    SalidaRepository salidaRepository, ReservaRepository reservaRepository,
                                    ParticipanteRepository participanteRepository, PagoRepository pagoRepository) {
        this.clienteRepository = clienteRepository;
        this.excursionRepository = excursionRepository;
        this.salidaRepository = salidaRepository;
        this.reservaRepository = reservaRepository;
        this.participanteRepository = participanteRepository;
        this.pagoRepository = pagoRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (reservaRepository.countByEstadoReservaIn(
                EnumSet.of(EstadoReserva.PENDIENTE_DEVOLUCION, EstadoReserva.DEVUELTA)) > 0
                || clienteRepository.existsByDni("36444555")) {
            return;
        }
        Map<String, Excursion> excursiones = excursionRepository.findAllByOrderByNombreAsc().stream()
                .collect(Collectors.toMap(Excursion::getNombre, Function.identity()));
        LocalDate hoy = LocalDate.now();

        Excursion perito = excursiones.get("Ascenso Cerro Perito Moreno");
        if (perito != null) {
            Salida cancelada = salida(perito, hoy.plusDays(5), LocalTime.of(8, 0), LocalTime.of(17, 0), 14);
            cancelada.setEstadoSalida(EstadoSalida.CANCELADA);
            cancelada.setMotivoCancelacion(MOTIVO_CLIMATICA);
            salidaRepository.save(cancelada);

            Cliente julian = cliente("36444555", "Julián", "Ferreyra", "julian.ferreyra@mail.com", "2804-551122", 1991);
            reserva(cancelada, julian, 2, perito.getPrecio(), true, MOTIVO_CLIMATICA, hoy.minusDays(18));

            Cliente melina = cliente("40222333", "Melina", "Cabral", "melina.cabral@mail.com", "2804-553344", 1997);
            reserva(cancelada, melina, 1, perito.getPrecio(), false, MOTIVO_CLIMATICA, hoy.minusDays(12));

            // Ya devuelta: muestra cómo queda el historial
            Cliente paula = cliente("41555666", "Paula", "Medina", "paula.medina@mail.com", "2804-559900", 1999);
            Reserva devuelta = reserva(cancelada, paula, 2, perito.getPrecio(), false, MOTIVO_CLIMATICA,
                    hoy.minusDays(20));
            devuelta.setEstadoReserva(EstadoReserva.DEVUELTA);
            pagoRepository.findByReservaIdReservaOrderByIdPagoAsc(devuelta.getIdReserva())
                    .forEach(p -> p.setEstadoPago(EstadoPago.DEVUELTO));
            pago(devuelta, devuelta.getMontoTotal(), ModoPago.REINTEGRO, "REI-DEMO" + String.format("%04d", devuelta.getIdReserva()),
                    LocalDateTime.now().minusDays(2).withHour(10).withMinute(20));
        }

        Excursion laguna = excursiones.get("Sendero Laguna del Bosque");
        if (laguna != null) {
            Salida reprogramada = salida(laguna, hoy.plusDays(16), LocalTime.of(9, 0), LocalTime.of(14, 0), 12);
            reprogramada.setObservaciones("Reprogramada por contingencia operativa: el guía responsable no estaba disponible en la fecha original.");
            salidaRepository.save(reprogramada);
            Cliente diego = cliente("32111999", "Diego", "Sosa", "diego.sosa@mail.com", "2804-557788", 1985);
            reserva(reprogramada, diego, 2, laguna.getPrecio(), false, MOTIVO_REPROGRAMACION, hoy.minusDays(9));
        }
    }

    private Salida salida(Excursion excursion, LocalDate dia, LocalTime desde, LocalTime hasta, int cupoTotal) {
        Salida s = new Salida();
        s.setExcursion(excursion);
        s.setFechaInicio(dia);
        s.setFechaFinalizacion(dia);
        s.setHoraInicio(desde);
        s.setHoraFinalizacion(hasta);
        s.setCupoBase(Math.min(8, cupoTotal));
        s.setCupoMaximo(cupoTotal);
        s.setCupoTotal(cupoTotal);
        // Los cupos de las reservas afectadas ya se liberaron (RF-14)
        s.setCuposOcupados(0);
        s.setEstadoSalida(EstadoSalida.DISPONIBLE);
        s.setRecorrido(excursion.getRecorridoBase());
        return salidaRepository.save(s);
    }

    /** Si ya existe un cliente con ese correo (por ejemplo, de otros datos de ejemplo), se reutiliza. */
    private Cliente cliente(String dni, String nombre, String apellido, String correo, String telefono, int anio) {
        return clienteRepository.findByCorreoElectronicoIgnoreCase(correo)
                .orElseGet(() -> clienteRepository.save(
                        new Cliente(dni, nombre, apellido, correo, telefono, LocalDate.of(anio, 8, 10))));
    }

    /** Reserva "Pendiente de devolución" con su pago aprobado (seña del 50 % o pago completo). */
    private Reserva reserva(Salida s, Cliente c, int cantidad, BigDecimal precio, boolean senada, String motivo,
                           LocalDate dia) {
        BigDecimal monto = precio.multiply(BigDecimal.valueOf(cantidad));
        BigDecimal abonado = senada ? monto.multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP) : monto;
        Reserva r = new Reserva();
        r.setSalida(s);
        r.setCliente(c);
        r.setFechaHora(dia.atTime(11, 0));
        r.setEstadoReserva(EstadoReserva.PENDIENTE_DEVOLUCION);
        r.setCantidadParticipantes(cantidad);
        r.setMontoTotal(monto);
        r.setSaldoPendiente(monto.subtract(abonado));
        r.setMotivoCancelacion(motivo);
        r = reservaRepository.save(r);
        participanteRepository.save(new Participante(r, c.getDni(), c.getNombre(), c.getApellido(),
                c.getCorreoElectronico(), c.getTelefono(), c.getFechaNacimiento()));
        pago(r, abonado, senada ? ModoPago.SENA : ModoPago.TOTAL,
                "SIM-DEMO" + String.format("%04d", r.getIdReserva()), r.getFechaHora().plusMinutes(4));
        return r;
    }

    private void pago(Reserva r, BigDecimal importe, ModoPago modo, String nro, LocalDateTime fecha) {
        Pago p = new Pago(r, importe, modo);
        p.setEstadoPago(EstadoPago.APROBADO);
        p.setNroTransaccion(nro);
        p.setFechaPago(fecha);
        pagoRepository.save(p);
    }
}
