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
import com.patatrek.backend.valoracion.Valoracion;
import com.patatrek.backend.valoracion.ValoracionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Datos de ejemplo para CU-28 / CU-29 (valoraciones):
 *  - Una salida FINALIZADA de "Sendero Laguna del Bosque" (hace 20 días) con:
 *      · la reserva de Sofía Ibarra (ella y Mateo Ibarra, Presente y Apto) → SIN valorar, para mostrarlo en la demo;
 *      · la reserva de otro cliente, ya valorada (5 y 4 estrellas) + un participante Ausente (no puede valorar).
 *  - Una valoración de Elena Castro en la salida finalizada del Perito Moreno (DatosInicialesGuia).
 * Se carga una sola vez: si ya hay valoraciones, no hace nada.
 */
@Component
@Order(4)
@ConditionalOnProperty(name = "patatrek.cargar-datos-demo", havingValue = "true", matchIfMissing = true)
public class DatosInicialesValoracion implements CommandLineRunner {

    private final ValoracionRepository valoracionRepository;
    private final ClienteRepository clienteRepository;
    private final ExcursionRepository excursionRepository;
    private final SalidaRepository salidaRepository;
    private final ReservaRepository reservaRepository;
    private final ParticipanteRepository participanteRepository;
    private final PagoRepository pagoRepository;
    private final String correoClienteDemo;

    public DatosInicialesValoracion(ValoracionRepository valoracionRepository, ClienteRepository clienteRepository,
                                    ExcursionRepository excursionRepository, SalidaRepository salidaRepository,
                                    ReservaRepository reservaRepository, ParticipanteRepository participanteRepository,
                                    PagoRepository pagoRepository,
                                    @Value("${patatrek.cliente-demo-correo:sofia.ibarra@mail.com}") String correoClienteDemo) {
        this.valoracionRepository = valoracionRepository;
        this.clienteRepository = clienteRepository;
        this.excursionRepository = excursionRepository;
        this.salidaRepository = salidaRepository;
        this.reservaRepository = reservaRepository;
        this.participanteRepository = participanteRepository;
        this.pagoRepository = pagoRepository;
        this.correoClienteDemo = correoClienteDemo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (valoracionRepository.count() > 0) {
            return;
        }
        Cliente sofia = clienteRepository.findByCorreoElectronicoIgnoreCase(correoClienteDemo).orElse(null);
        Excursion laguna = excursionRepository.findAllByOrderByNombreAsc().stream()
                .filter(e -> e.getNombre().equals("Sendero Laguna del Bosque")).findFirst().orElse(null);

        if (sofia != null && laguna != null) {
            LocalDate dia = LocalDate.now().minusDays(20);
            Salida s = new Salida();
            s.setExcursion(laguna);
            s.setFechaInicio(dia);
            s.setFechaFinalizacion(dia);
            s.setHoraInicio(LocalTime.of(9, 0));
            s.setHoraFinalizacion(LocalTime.of(14, 0));
            s.setCupoBase(8);
            s.setCupoMaximo(12);
            s.setCupoTotal(12);
            s.setCuposOcupados(5);
            s.setEstadoSalida(EstadoSalida.FINALIZADA);
            s.setFechaHoraFinReal(dia.atTime(13, 50));
            s.setRecorrido(laguna.getRecorridoBase());
            s.setObservaciones("Día soleado, el grupo completó el circuito sin inconvenientes.");
            salidaRepository.save(s);

            // Reserva de Sofía: los dos Presente y Apto, todavía sin valorar
            Reserva rs = reserva(s, sofia, 2, laguna.getPrecio(), dia);
            participante(rs, sofia.getDni(), sofia.getNombre(), sofia.getApellido(), sofia.getCorreoElectronico(),
                    sofia.getTelefono(), 1994, EstadoAsistencia.PRESENTE, EstadoRequisitos.APTO);
            participante(rs, "45111222", "Mateo", "Ibarra", "mateo.ibarra@mail.com", "2804-667790", 1998,
                    EstadoAsistencia.PRESENTE, EstadoRequisitos.APTO);

            // Reserva de otro cliente, ya valorada
            Cliente otro = clienteRepository.save(new Cliente("33221144", "Marcos", "Quiroga",
                    "marcos.quiroga@mail.com", "11-6655-4433", LocalDate.of(1988, 4, 2)));
            Reserva ro = reserva(s, otro, 3, laguna.getPrecio(), dia);
            Participante marcos = participante(ro, "33221144", "Marcos", "Quiroga", "marcos.quiroga@mail.com",
                    "11-6655-4433", 1988, EstadoAsistencia.PRESENTE, EstadoRequisitos.APTO);
            Participante lara = participante(ro, "46778899", "Lara", "Quiroga", "lara.quiroga@mail.com",
                    "11-6655-4434", 2005, EstadoAsistencia.PRESENTE, EstadoRequisitos.APTO);
            participante(ro, "34990011", "Nicolás", "Paz", "nico.paz@mail.com", "11-6655-4435", 1990,
                    EstadoAsistencia.AUSENTE, null);
            valoracionRepository.save(new Valoracion(marcos, 5,
                    "Excelente organización y el guía muy atento. Ideal para ir en familia.", dia.plusDays(1).atTime(10, 15)));
            valoracionRepository.save(new Valoracion(lara, 4,
                    "Muy lindo el recorrido, la laguna es hermosa. Faltó un poco más de tiempo para fotos.",
                    dia.plusDays(2).atTime(19, 40)));
        }

        // Valoración en la salida finalizada del Perito Moreno (cargada por DatosInicialesGuia)
        participanteRepository.findByDniAndReservaSalidaEstadoSalida("34221100", EstadoSalida.FINALIZADA).stream()
                .filter(p -> p.getEstadoAsistencia() == EstadoAsistencia.PRESENTE
                        && p.getEstadoRequisitos() == EstadoRequisitos.APTO)
                .findFirst()
                .ifPresent(p -> valoracionRepository.save(new Valoracion(p, 4,
                        "Exigente pero vale cada paso. La vista desde la cumbre es impresionante.",
                        p.getReserva().getSalida().getFechaInicio().plusDays(1).atTime(9, 30))));
    }

    private Reserva reserva(Salida s, Cliente c, int cantidad, BigDecimal precio, LocalDate dia) {
        BigDecimal monto = precio.multiply(BigDecimal.valueOf(cantidad));
        Reserva r = new Reserva();
        r.setSalida(s);
        r.setCliente(c);
        r.setFechaHora(dia.minusDays(15).atTime(11, 0));
        r.setEstadoReserva(EstadoReserva.CONFIRMADA);
        r.setCantidadParticipantes(cantidad);
        r.setMontoTotal(monto);
        r.setSaldoPendiente(BigDecimal.ZERO);
        r = reservaRepository.save(r);
        Pago pago = new Pago(r, monto, ModoPago.TOTAL);
        pago.setEstadoPago(EstadoPago.APROBADO);
        pago.setNroTransaccion("SIM-DEMO" + String.format("%04d", r.getIdReserva()));
        pago.setFechaPago(r.getFechaHora().plusMinutes(3));
        pagoRepository.save(pago);
        return r;
    }

    private Participante participante(Reserva r, String dni, String nombre, String apellido, String correo,
                                      String telefono, int anio, EstadoAsistencia asistencia, EstadoRequisitos requisitos) {
        Participante p = new Participante(r, dni, nombre, apellido, correo, telefono, LocalDate.of(anio, 6, 15));
        p.setEstadoAsistencia(asistencia);
        p.setEstadoRequisitos(requisitos);
        return participanteRepository.save(p);
    }
}
