package com.patatrek.backend.reserva;

import com.patatrek.backend.certificado.CertificadoMedico;
import com.patatrek.backend.certificado.CertificadoMedicoRepository;
import com.patatrek.backend.certificado.EstadoCertificado;
import com.patatrek.backend.notificacion.Correo;
import com.patatrek.backend.notificacion.FormatoCorreo;
import com.patatrek.backend.notificacion.NotificacionService;
import com.patatrek.backend.reserva.dto.ResultadoCancelacionAutomaticaResponse;
import com.patatrek.backend.reserva.dto.ResultadoCancelacionAutomaticaResponse.ReservaCancelada;
import com.patatrek.backend.salida.EstadoSalida;
import com.patatrek.backend.salida.Salida;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CU-30 Cancelar reservas automáticamente (actor: Temporizador).
 *
 * Revisa las salidas Disponibles o Completas que empiezan dentro de las próximas 72 horas y cancela
 * sus reservas Señadas o Confirmadas que incumplen alguna condición:
 *  - siguen "Señadas": no se pagó el saldo restante antes del plazo límite;
 *  - la excursión exige certificado médico y ningún participante de la reserva tiene uno Cargado o Aprobado.
 * Por cada reserva cancelada libera el cupo, las plazas de refugio y el equipamiento, registra el motivo
 * y le avisa por correo al cliente titular. No hay devolución de lo abonado (regla de negocio del CU-30).
 * Si una reserva Confirmada tiene al menos un participante con certificado Cargado o Aprobado, no se cancela:
 * los que no lo tengan quedan No aptos en el control de asistencia del guía (CU-15).
 */
@Service
public class CancelacionAutomaticaService {

    private static final Logger log = LoggerFactory.getLogger(CancelacionAutomaticaService.class);
    static final long HORAS_PLAZO = ReservaService.HORAS_MINIMAS_ANTICIPACION;

    static final String MOTIVO_SALDO =
            "No se completó el pago del saldo restante antes del plazo límite (72 horas antes de la salida).";
    static final String MOTIVO_CERTIFICADO =
            "La excursión exige certificado médico y ningún participante de la reserva presentó uno válido "
                    + "antes del plazo límite (72 horas antes de la salida).";

    private final EntityManager em;
    private final ReservaRepository reservaRepository;
    private final ParticipanteRepository participanteRepository;
    private final CertificadoMedicoRepository certificadoRepository;
    private final ReservaService reservaService;
    private final NotificacionService notificacionService;

    public CancelacionAutomaticaService(EntityManager em, ReservaRepository reservaRepository,
                                        ParticipanteRepository participanteRepository,
                                        CertificadoMedicoRepository certificadoRepository,
                                        ReservaService reservaService, NotificacionService notificacionService) {
        this.em = em;
        this.reservaRepository = reservaRepository;
        this.participanteRepository = participanteRepository;
        this.certificadoRepository = certificadoRepository;
        this.reservaService = reservaService;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public ResultadoCancelacionAutomaticaResponse ejecutar() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = ahora.plusHours(HORAS_PLAZO);

        // Paso 2: salidas a las que les faltan 3 días o menos (y todavía no empezaron)
        List<Salida> salidas = em.createQuery("""
                        select s from Salida s join fetch s.excursion
                        where s.estadoSalida in :estados and s.fechaInicio between :desde and :hasta
                        order by s.fechaInicio, s.horaInicio""", Salida.class)
                .setParameter("estados", EnumSet.of(EstadoSalida.DISPONIBLE, EstadoSalida.COMPLETA))
                .setParameter("desde", ahora.toLocalDate())
                .setParameter("hasta", limite.toLocalDate())
                .getResultList().stream()
                .filter(s -> {
                    LocalDateTime inicio = s.getFechaInicio().atTime(s.getHoraInicio());
                    return inicio.isAfter(ahora) && !inicio.isAfter(limite);
                })
                .toList();

        int reservasRevisadas = 0;
        List<ReservaCancelada> canceladas = new ArrayList<>();
        for (Salida s : salidas) {
            // Bloqueo de la salida: se modifican sus cupos (igual que al reservar o cancelar)
            Salida salida = em.find(Salida.class, s.getIdSalida(), LockModeType.PESSIMISTIC_WRITE);
            List<Reserva> reservas = reservaRepository.findBySalidaIdSalidaAndEstadoReservaInOrderByIdReservaAsc(
                    salida.getIdSalida(), EstadoReserva.VIGENTES);
            for (Reserva reserva : reservas) {
                reservasRevisadas++;
                // Paso 3: condiciones de cancelación
                String motivo = motivoDeCancelacion(reserva, salida);
                if (motivo == null) {
                    continue;
                }
                // Pasos 4 y 5: liberar recursos (si la salida estaba Completa, vuelve a Disponible) y cancelar
                List<String> liberado = reservaService.liberarRecursosConsolidados(reserva, salida);
                reserva.setEstadoReserva(EstadoReserva.CANCELADA);
                reserva.setMotivoCancelacion("Cancelación automática: " + motivo);
                // Paso 6: aviso al cliente titular
                notificar(reserva, salida, motivo, liberado);
                canceladas.add(new ReservaCancelada(reserva.getIdReserva(),
                        reserva.getCliente().getApellido() + ", " + reserva.getCliente().getNombre(),
                        salida.getExcursion().getNombre(), salida.getIdSalida(), motivo));
                log.info("CU-30: reserva #{} cancelada automáticamente. {}", reserva.getIdReserva(), motivo);
            }
        }

        // Flujo alternativo 5a: ninguna reserva incumple las condiciones
        String mensaje = canceladas.isEmpty()
                ? "No hay reservas que incumplan las condiciones: no se canceló ninguna."
                : canceladas.size() == 1
                ? "Se canceló 1 reserva y se avisó por correo a su titular."
                : "Se cancelaron " + canceladas.size() + " reservas y se avisó por correo a cada titular.";
        return new ResultadoCancelacionAutomaticaResponse(ahora, salidas.size(), reservasRevisadas, canceladas, mensaje);
    }

    /** null si la reserva cumple las condiciones; si no, el motivo de la cancelación. */
    private String motivoDeCancelacion(Reserva reserva, Salida salida) {
        List<String> motivos = new ArrayList<>();
        if (reserva.getEstadoReserva() == EstadoReserva.SENADA) {
            motivos.add(MOTIVO_SALDO);
        }
        if (salida.getExcursion().isRequiereCertificado() && !tieneCertificadoValido(reserva)) {
            motivos.add(MOTIVO_CERTIFICADO);
        }
        return motivos.isEmpty() ? null : String.join(" ", motivos);
    }

    /** Al menos un participante con certificado Cargado o Aprobado. */
    private boolean tieneCertificadoValido(Reserva reserva) {
        List<Long> ids = participanteRepository.findByReservaIdReservaOrderByIdParticipanteAsc(reserva.getIdReserva())
                .stream().map(Participante::getIdParticipante).toList();
        if (ids.isEmpty()) {
            return false;
        }
        return certificadoRepository.findByParticipanteIdParticipanteIn(ids).stream()
                .map(CertificadoMedico::getEstadoCertificado)
                .anyMatch(e -> e == EstadoCertificado.CARGADO || e == EstadoCertificado.APROBADO);
    }

    private void notificar(Reserva r, Salida s, String motivo, List<String> liberado) {
        BigDecimal abonado = r.getMontoTotal().subtract(r.getSaldoPendiente()).max(BigDecimal.ZERO);
        notificacionService.enviar(Correo.para(r.getCliente().getCorreoElectronico())
                .asunto("PataTrek — Tu reserva #" + r.getIdReserva() + " fue cancelada")
                .titulo("Tu reserva fue cancelada")
                .saludo("Hola " + r.getCliente().getNombre() + ":")
                .parrafo("Te informamos que tu reserva se canceló automáticamente porque, al llegar el plazo límite "
                        + "de 72 horas antes de la salida, no cumplía las condiciones necesarias para participar.")
                .dato("Reserva", "#" + r.getIdReserva() + " — " + s.getExcursion().getNombre())
                .dato("Salida", FormatoCorreo.salida(s.getFechaInicio(), s.getFechaFinalizacion(), s.getHoraInicio()))
                .dato("Motivo", motivo)
                .dato("Servicios liberados", String.join(" · ", liberado))
                .datoDestacado("Importe abonado", FormatoCorreo.pesos(abonado))
                .nota("Por tratarse de un incumplimiento de los requisitos en el plazo establecido, el importe abonado "
                        + "no se devuelve. Si querés, podés reservar otra salida desde el catálogo.")
                .boton("Ver excursiones", notificacionService.enlace("/"))
                .build());
    }
}
