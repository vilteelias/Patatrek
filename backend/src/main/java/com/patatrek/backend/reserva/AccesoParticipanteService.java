package com.patatrek.backend.reserva;

import com.patatrek.backend.excepcion.DatosInvalidosException;
import com.patatrek.backend.excepcion.ReglaNegocioException;
import com.patatrek.backend.pago.ModoPago;
import com.patatrek.backend.reserva.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CU-19 Consultar y gestionar reserva, del lado del Participante (sin cuenta, con ID de reserva + DNI).
 * Desde acá el participante ve su reserva, carga su certificado (CU-24) y puede pagar el saldo restante (CU-26 4b).
 *
 * Reglas de CU-19: solo acceden participantes incluidos en la reserva; con dos intentos consecutivos fallidos
 * el acceso a esa reserva se bloquea 5 minutos (flujo 2b).
 */
@Service
public class AccesoParticipanteService {

    static final int INTENTOS_PERMITIDOS = 2;
    static final Duration BLOQUEO = Duration.ofMinutes(5);
    private static final String DATOS_INCORRECTOS = "Los datos ingresados no son correctos. Revisá el N.º de reserva y tu DNI.";

    private final ReservaService reservaService;
    private final ReservaRepository reservaRepository;
    private final ParticipanteRepository participanteRepository;

    /** Intentos fallidos consecutivos por N.º de reserva (en memoria: alcanza para un bloqueo de minutos). */
    private final Map<Long, Intentos> intentos = new ConcurrentHashMap<>();

    private record Intentos(int fallidos, LocalDateTime bloqueadoHasta) {}

    public AccesoParticipanteService(ReservaService reservaService, ReservaRepository reservaRepository,
                                     ParticipanteRepository participanteRepository) {
        this.reservaService = reservaService;
        this.reservaRepository = reservaRepository;
        this.participanteRepository = participanteRepository;
    }

    /** Pasos 1-3 de CU-19: valida el ID de reserva + DNI y muestra la reserva al participante. */
    @Transactional
    public ParticipanteReservaResponse consultar(Long idReserva, String dni) {
        Participante p = validarAcceso(idReserva, dni);
        return vista(reservaService.obtener(idReserva), p);
    }

    /** CU-19 3b → CU-26 4b: el participante paga el saldo restante de una reserva señada. */
    @Transactional
    public ResultadoPagoParticipanteResponse pagarSaldo(Long idReserva, PagoParticipanteRequest datos) {
        Participante p = validarAcceso(idReserva, datos.dni());
        ReservaResponse actual = reservaService.obtener(idReserva);
        if (!actual.modosPagoDisponibles().contains(ModoPago.SALDO)) {
            throw new ReglaNegocioException(actual.motivoNoPuedePagar() != null
                    ? actual.motivoNoPuedePagar()
                    : "Esta reserva no tiene saldo pendiente para pagar.");
        }
        ResultadoPagoResponse r = reservaService.pagar(idReserva, new PagoRequest(ModoPago.SALDO, datos.aprobar()));
        return new ResultadoPagoParticipanteResponse(r.idPago(), r.estadoPago(), r.nroTransaccion(),
                r.mensaje(), vista(r.reserva(), p));
    }

    /** Flujos 2a y 2b de CU-19. */
    private Participante validarAcceso(Long idReserva, String dni) {
        LocalDateTime ahora = LocalDateTime.now();
        Intentos previo = intentos.get(idReserva);
        if (previo != null && previo.bloqueadoHasta() != null && ahora.isBefore(previo.bloqueadoHasta())) {
            long minutos = Math.max(1, (Duration.between(ahora, previo.bloqueadoHasta()).toSeconds() + 59) / 60);
            throw new ReglaNegocioException("Superaste los intentos permitidos. Podés volver a intentar en "
                    + minutos + (minutos == 1 ? " minuto." : " minutos."));
        }
        String limpio = dni == null ? "" : dni.replaceAll("\\D", "");
        Participante p = limpio.isEmpty() || !reservaRepository.existsById(idReserva) ? null
                : participanteRepository.findByReservaIdReservaAndDni(idReserva, limpio).orElse(null);
        if (p == null) {
            int fallidos = (previo == null || previo.bloqueadoHasta() != null ? 0 : previo.fallidos()) + 1;
            if (fallidos >= INTENTOS_PERMITIDOS) {
                intentos.put(idReserva, new Intentos(fallidos, ahora.plus(BLOQUEO)));
                throw new ReglaNegocioException("Los datos no son correctos y superaste los intentos permitidos. "
                        + "Podés volver a intentar en " + BLOQUEO.toMinutes() + " minutos.");
            }
            intentos.put(idReserva, new Intentos(fallidos, null));
            throw new DatosInvalidosException(DATOS_INCORRECTOS, Map.of());
        }
        intentos.remove(idReserva);
        return p;
    }

    private static ParticipanteReservaResponse vista(ReservaResponse r, Participante p) {
        // El participante solo puede pagar el saldo restante: la seña y el pago total los hace el titular.
        List<ModoPago> modos = r.modosPagoDisponibles().contains(ModoPago.SALDO) ? List.of(ModoPago.SALDO) : List.of();
        String motivo = modos.isEmpty()
                ? (r.motivoNoPuedePagar() != null ? r.motivoNoPuedePagar() : "La reserva no tiene saldo pendiente.")
                : null;
        Reserva reserva = p.getReserva();
        String titular = reserva.getCliente().getApellido() + ", " + reserva.getCliente().getNombre();
        return new ParticipanteReservaResponse(r.idReserva(), r.estadoReserva(), titular,
                new ParticipanteReservaResponse.ParticipanteActual(p.getDni(), p.getNombre(), p.getApellido()),
                r.cantidadParticipantes(), r.montoTotal(), r.importeSena(), r.saldoPendiente(), modos, motivo,
                r.salida(),
                r.participantes().stream().map(x -> x.apellido() + ", " + x.nombre()).toList(),
                r.motivoCancelacion());
    }
}
