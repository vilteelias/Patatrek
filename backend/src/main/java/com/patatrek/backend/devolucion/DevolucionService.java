package com.patatrek.backend.devolucion;

import com.patatrek.backend.devolucion.dto.DevolucionResponse;
import com.patatrek.backend.devolucion.dto.DevolucionesResponse;
import com.patatrek.backend.devolucion.dto.ResultadoDevolucionResponse;
import com.patatrek.backend.excepcion.RecursoNoEncontradoException;
import com.patatrek.backend.excepcion.ReglaNegocioException;
import com.patatrek.backend.notificacion.NotificacionService;
import com.patatrek.backend.pago.*;
import com.patatrek.backend.reserva.EstadoReserva;
import com.patatrek.backend.reserva.Reserva;
import com.patatrek.backend.reserva.ReservaRepository;
import com.patatrek.backend.salida.Salida;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Paquete "Pagos y Devoluciones" (Documento de Arquitectura) — CU-27 Registrar devolución de reserva (RF-24).
 *
 * Las reservas llegan a "Pendiente de devolución" cuando la empresa cancela la salida por una contingencia
 * o cuando el cliente rechaza una reprogramación (CU-14). Acá el administrador ejecuta el reembolso total
 * de lo efectivamente abonado (la seña o el pago completo) a través de la pasarela simulada.
 *
 * Igual que en CU-26, PataTrek no guarda ningún dato de tarjeta: solo el importe, el modo (REINTEGRO),
 * el estado y el número de transacción que devuelve la pasarela.
 */
@Service
public class DevolucionService {

    private static final Logger log = LoggerFactory.getLogger(DevolucionService.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** $ 85.000 (sin decimales, como en las pantallas). */
    private static String pesos(BigDecimal importe) {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR"));
        f.setMaximumFractionDigits(0);
        return "$ " + f.format(importe);
    }

    private final EntityManager em;
    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final PasarelaPagosSimulada pasarela;
    private final NotificacionService notificacionService;

    public DevolucionService(EntityManager em, ReservaRepository reservaRepository, PagoRepository pagoRepository,
                             PasarelaPagosSimulada pasarela, NotificacionService notificacionService) {
        this.em = em;
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.pasarela = pasarela;
        this.notificacionService = notificacionService;
    }

    // =====================================================================
    //  Pasos 1-2: listado de reservas con devolución pendiente (2a: lista vacía)
    // =====================================================================

    @Transactional(readOnly = true)
    public DevolucionesResponse listar() {
        List<Reserva> reservas = reservaRepository.findByEstadoReservaInOrderByIdReservaAsc(
                EnumSet.of(EstadoReserva.PENDIENTE_DEVOLUCION, EstadoReserva.DEVUELTA));
        Map<Long, List<Pago>> pagos = new HashMap<>();
        if (!reservas.isEmpty()) {
            pagoRepository.findByReservaIdReservaIn(reservas.stream().map(Reserva::getIdReserva).toList())
                    .forEach(p -> pagos.computeIfAbsent(p.getReserva().getIdReserva(), k -> new ArrayList<>()).add(p));
            pagos.values().forEach(l -> l.sort(Comparator.comparing(Pago::getIdPago)));
        }

        List<DevolucionResponse> pendientes = new ArrayList<>();
        List<DevolucionResponse> realizadas = new ArrayList<>();
        for (Reserva r : reservas) {
            DevolucionResponse fila = fila(r, pagos.getOrDefault(r.getIdReserva(), List.of()));
            (r.getEstadoReserva() == EstadoReserva.PENDIENTE_DEVOLUCION ? pendientes : realizadas).add(fila);
        }
        // Las devueltas, de la más reciente a la más vieja
        realizadas.sort(Comparator.comparing(DevolucionResponse::fechaReintegro,
                Comparator.nullsLast(Comparator.reverseOrder())));
        BigDecimal total = pendientes.stream().map(DevolucionResponse::importeAbonado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DevolucionesResponse(pendientes, total, realizadas);
    }

    // =====================================================================
    //  Pasos 3-9: ejecutar la devolución total
    // =====================================================================

    @Transactional
    public ResultadoDevolucionResponse devolver(Long idReserva) {
        // Bloqueo de la reserva: evita que dos administradores reintegren lo mismo dos veces.
        Reserva reserva = em.find(Reserva.class, idReserva, LockModeType.PESSIMISTIC_WRITE);
        if (reserva == null) {
            throw new RecursoNoEncontradoException("No existe la reserva #" + idReserva + ".");
        }
        if (reserva.getEstadoReserva() == EstadoReserva.DEVUELTA) {
            throw new ReglaNegocioException("La reserva #" + idReserva + " ya fue devuelta.");
        }
        if (reserva.getEstadoReserva() != EstadoReserva.PENDIENTE_DEVOLUCION) {
            throw new ReglaNegocioException("Solo se pueden devolver reservas en estado \"Pendiente de devolución\".");
        }

        // Precondición: al menos un pago aprobado. Regla de negocio: se reintegra todo lo abonado.
        List<Pago> aprobados = pagosAprobados(pagoRepository.findByReservaIdReservaOrderByIdPagoAsc(idReserva));
        if (aprobados.isEmpty()) {
            throw new ReglaNegocioException("La reserva #" + idReserva + " no tiene pagos aprobados para reintegrar.");
        }
        BigDecimal importe = aprobados.stream().map(Pago::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
        String nroOriginal = aprobados.get(aprobados.size() - 1).getNroTransaccion();

        // Paso 4: transacción de reembolso "Pendiente" por el total abonado
        Pago reintegro = pagoRepository.save(new Pago(reserva, importe, ModoPago.REINTEGRO));

        // Pasos 5-6: la pasarela procesa el reembolso y devuelve el número de transacción de reintegro
        PasarelaPagosSimulada.Respuesta r = pasarela.reintegrar(reintegro, nroOriginal);
        reintegro.setNroTransaccion(r.nroTransaccion());
        reintegro.setFechaPago(LocalDateTime.now());
        if (!r.aprobado()) {
            reintegro.setEstadoPago(EstadoPago.RECHAZADO);
            throw new ReglaNegocioException("La pasarela de pagos no pudo procesar el reintegro. Intentá de nuevo más tarde.");
        }
        reintegro.setEstadoPago(EstadoPago.APROBADO);
        aprobados.forEach(p -> p.setEstadoPago(EstadoPago.DEVUELTO));

        // Paso 7
        reserva.setEstadoReserva(EstadoReserva.DEVUELTA);

        // Paso 8: comprobante al titular (fecha, motivo, ID de reserva y monto reintegrado — RF-24)
        enviarComprobante(reserva, reintegro, nroOriginal);
        log.info("Reserva #{} devuelta: $ {} ({})", idReserva, importe.toPlainString(), r.nroTransaccion());

        // Paso 9
        List<Pago> todos = new ArrayList<>(aprobados);
        todos.add(reintegro);
        String mensaje = "Se reintegraron %s a %s %s (reserva #%d). Le enviamos el comprobante por correo."
                .formatted(pesos(importe), reserva.getCliente().getNombre(), reserva.getCliente().getApellido(),
                        idReserva);
        return new ResultadoDevolucionResponse(idReserva, importe, r.nroTransaccion(), mensaje, fila(reserva, todos));
    }

    // =====================================================================

    /** Pagos que suman lo abonado: los aprobados de seña, total o saldo (sin contar reintegros). */
    private static List<Pago> pagosAprobados(List<Pago> pagos) {
        return pagos.stream()
                .filter(p -> p.getModoPago() != ModoPago.REINTEGRO && p.getEstadoPago() == EstadoPago.APROBADO)
                .toList();
    }

    private static DevolucionResponse fila(Reserva r, List<Pago> pagos) {
        Salida s = r.getSalida();
        Pago reintegro = pagos.stream()
                .filter(p -> p.getModoPago() == ModoPago.REINTEGRO && p.getEstadoPago() == EstadoPago.APROBADO)
                .reduce((a, b) -> b).orElse(null);
        // Lo abonado: los pagos aprobados (o ya devueltos) de seña, total o saldo
        List<Pago> abonados = pagos.stream()
                .filter(p -> p.getModoPago() != ModoPago.REINTEGRO
                        && (p.getEstadoPago() == EstadoPago.APROBADO || p.getEstadoPago() == EstadoPago.DEVUELTO))
                .toList();
        BigDecimal abonado = reintegro != null ? reintegro.getImporte()
                : abonados.stream().map(Pago::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
        Pago ultimo = abonados.isEmpty() ? null : abonados.get(abonados.size() - 1);
        String motivo = r.getMotivoCancelacion() != null ? r.getMotivoCancelacion() : s.getMotivoCancelacion();
        return new DevolucionResponse(
                r.getIdReserva(), r.getEstadoReserva(),
                r.getCliente().getApellido() + ", " + r.getCliente().getNombre(),
                r.getCliente().getDni(), r.getCliente().getCorreoElectronico(),
                s.getExcursion().getNombre(), s.getIdSalida(), s.getFechaInicio(), s.getFechaFinalizacion(),
                s.getEstadoSalida(), r.getCantidadParticipantes(),
                motivo == null ? "Sin motivo registrado." : motivo,
                abonado,
                ultimo == null ? null : ultimo.getFechaPago(),
                ultimo == null ? null : ultimo.getNroTransaccion(),
                reintegro == null ? null : reintegro.getNroTransaccion(),
                reintegro == null ? null : reintegro.getFechaPago());
    }

    private void enviarComprobante(Reserva r, Pago reintegro, String nroOriginal) {
        Salida s = r.getSalida();
        String motivo = r.getMotivoCancelacion() != null ? r.getMotivoCancelacion() : s.getMotivoCancelacion();
        String cuerpo = """
                ¡Hola %s!

                Te devolvimos el total de lo que abonaste por tu reserva. Este es el comprobante:

                Reserva: #%d — %s (salida del %s)
                Motivo: %s
                Monto reintegrado: %s
                Fecha del reintegro: %s
                N.º de transacción de reintegro: %s
                Transacción original: %s

                El dinero vuelve al mismo medio con el que pagaste. Según tu banco o billetera,
                puede tardar unos días hábiles en verse reflejado.

                Lamentamos los inconvenientes. ¡Esperamos verte pronto en otra salida de PataTrek!
                """.formatted(r.getCliente().getNombre(), r.getIdReserva(), s.getExcursion().getNombre(),
                s.getFechaInicio().format(FECHA), motivo == null ? "(no informado)" : motivo,
                pesos(reintegro.getImporte()), reintegro.getFechaPago().format(FECHA_HORA),
                reintegro.getNroTransaccion(), nroOriginal == null ? "-" : nroOriginal);
        notificacionService.enviar(r.getCliente().getCorreoElectronico(),
                "PataTrek — Comprobante de devolución de la reserva #" + r.getIdReserva(), cuerpo);
    }
}
