package com.patatrek.backend.config;

import com.patatrek.backend.certificado.CertificadoMedico;
import com.patatrek.backend.certificado.CertificadoMedicoRepository;
import com.patatrek.backend.certificado.EstadoCertificado;
import com.patatrek.backend.equipamiento.Equipamiento;
import com.patatrek.backend.equipamiento.EquipamientoRepository;
import com.patatrek.backend.excursion.Excursion;
import com.patatrek.backend.excursion.ExcursionRepository;
import com.patatrek.backend.reserva.*;
import com.patatrek.backend.salida.*;
import com.patatrek.backend.usuario.Cliente;
import com.patatrek.backend.usuario.ClienteRepository;
import com.patatrek.backend.usuario.Guia;
import com.patatrek.backend.usuario.GuiaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Datos de ejemplo para mostrar la parte del Guía (CU-10, CU-22, CU-15, CU-16):
 * guías, un refugio, clientes, salidas en distintos estados, reservas, participantes y certificados.
 * Se carga una sola vez (si no hay guías) y después de las excursiones de ejemplo.
 * Las fechas se calculan desde HOY, para que siempre haya una salida lista para el control de asistencia.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "patatrek.cargar-datos-demo", havingValue = "true", matchIfMissing = true)
public class DatosInicialesGuia implements CommandLineRunner {

    private final GuiaRepository guiaRepository;
    private final ClienteRepository clienteRepository;
    private final RefugioRepository refugioRepository;
    private final ExcursionRepository excursionRepository;
    private final EquipamientoRepository equipamientoRepository;
    private final SalidaRepository salidaRepository;
    private final AsignacionGuiaRepository asignacionGuiaRepository;
    private final AsignacionRefugioRepository asignacionRefugioRepository;
    private final ReservaRepository reservaRepository;
    private final ParticipanteRepository participanteRepository;
    private final CertificadoMedicoRepository certificadoRepository;
    private final AsignacionEquipamientoRepository asignacionEquipamientoRepository;

    public DatosInicialesGuia(GuiaRepository guiaRepository, ClienteRepository clienteRepository,
                              RefugioRepository refugioRepository, ExcursionRepository excursionRepository,
                              EquipamientoRepository equipamientoRepository, SalidaRepository salidaRepository,
                              AsignacionGuiaRepository asignacionGuiaRepository,
                              AsignacionRefugioRepository asignacionRefugioRepository,
                              ReservaRepository reservaRepository, ParticipanteRepository participanteRepository,
                              CertificadoMedicoRepository certificadoRepository,
                              AsignacionEquipamientoRepository asignacionEquipamientoRepository) {
        this.guiaRepository = guiaRepository;
        this.clienteRepository = clienteRepository;
        this.refugioRepository = refugioRepository;
        this.excursionRepository = excursionRepository;
        this.equipamientoRepository = equipamientoRepository;
        this.salidaRepository = salidaRepository;
        this.asignacionGuiaRepository = asignacionGuiaRepository;
        this.asignacionRefugioRepository = asignacionRefugioRepository;
        this.reservaRepository = reservaRepository;
        this.participanteRepository = participanteRepository;
        this.certificadoRepository = certificadoRepository;
        this.asignacionEquipamientoRepository = asignacionEquipamientoRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (guiaRepository.count() > 0) {
            return;
        }

        LocalDate hoy = LocalDate.now();
        Map<String, Excursion> excursiones = excursionRepository.findAllByOrderByNombreAsc().stream()
                .collect(Collectors.toMap(Excursion::getNombre, Function.identity()));
        Map<String, Equipamiento> equipo = equipamientoRepository.findAll().stream()
                .collect(Collectors.toMap(Equipamiento::getNombre, Function.identity()));

        // ---------- Guías ----------
        Guia almeida = guiaRepository.save(new Guia("28456123", "Martín", "Almeida",
                "martin.almeida@patatrek.com", "2804-551122", 8));
        Guia coronel = guiaRepository.save(new Guia("31877654", "Brenda", "Coronel",
                "brenda.coronel@patatrek.com", "2804-553344", 6));
        Guia ibarra = guiaRepository.save(new Guia("29333222", "Lucas", "Ibarra",
                "lucas.ibarra@patatrek.com", "2804-556677", 6));

        Refugio refugioCerroAlto = refugioRepository.save(
                new Refugio("Refugio Cerro Alto", 30, new BigDecimal("25000")));

        // ---------- 1) Hoy, Disponible: lista para el CONTROL DE ASISTENCIA (CU-15) ----------
        Optional.ofNullable(excursiones.get("Ascenso Cerro Perito Moreno")).ifPresent(exc -> {
            Salida s = salida(exc, hoy, hoy, LocalTime.of(8, 0), LocalTime.of(17, 0), 8, 14, 14, 4, EstadoSalida.DISPONIBLE);
            asignar(almeida, s, TipoGuia.RESPONSABLE);
            asignar(coronel, s, TipoGuia.AYUDANTE);

            Reserva a = reserva(s, cliente("36108442", "Rocío", "Delgado", "rocio.delgado@mail.com", "11-3344-5566"),
                    EstadoReserva.CONFIRMADA, 2, "190000", "0");
            certificado(participante(a, "36108442", "Rocío", "Delgado", "rocio.delgado@mail.com", "11-3344-5566", 1991),
                    EstadoCertificado.APROBADO);
            certificado(participante(a, "40221093", "Tomás", "Vera", "tvera@mail.com", "11-2233-8899", 1997),
                    EstadoCertificado.APROBADO);
            pedirEquipamiento(equipo.get("Casco"), a, 1);

            Reserva b = reserva(s, cliente("41902334", "Bruno", "Molina", "bmolina@mail.com", "11-5566-2211"),
                    EstadoReserva.SENADA, 1, "95000", "47500");
            certificado(participante(b, "41902334", "Bruno", "Molina", "bmolina@mail.com", "11-5566-2211", 1999),
                    EstadoCertificado.CARGADO);

            Reserva c = reserva(s, cliente("35660221", "Agustina", "Paredes", "a.paredes@mail.com", "11-7788-1122"),
                    EstadoReserva.CONFIRMADA, 1, "95000", "0");
            participante(c, "35660221", "Agustina", "Paredes", "a.paredes@mail.com", "11-7788-1122", 1990); // sin certificado
        });

        // ---------- 2) Hoy, En curso (control ya hecho): lista para MARCAR FINALIZACIÓN (CU-16) ----------
        Optional.ofNullable(excursiones.get("Sendero Laguna del Bosque")).ifPresent(exc -> {
            Salida s = salida(exc, hoy, hoy, LocalTime.of(9, 0), LocalTime.of(14, 0), 8, 8, 8, 3, EstadoSalida.EN_CURSO);
            asignar(almeida, s, TipoGuia.RESPONSABLE);

            Reserva d = reserva(s, cliente("33445566", "Julieta", "Sosa", "julieta.sosa@mail.com", "2804-112233"),
                    EstadoReserva.CONFIRMADA, 2, "84000", "0");
            acreditar(participante(d, "33445566", "Julieta", "Sosa", "julieta.sosa@mail.com", "2804-112233", 1988),
                    EstadoAsistencia.PRESENTE, EstadoRequisitos.APTO);
            acreditar(participante(d, "55667788", "Mateo", "Sosa", "julieta.sosa@mail.com", "2804-112233", 2015),
                    EstadoAsistencia.PRESENTE, EstadoRequisitos.APTO);

            Reserva e = reserva(s, cliente("38990011", "Camila", "Ríos", "camila.rios@mail.com", "2804-445566"),
                    EstadoReserva.CONFIRMADA, 1, "42000", "0");
            acreditar(participante(e, "38990011", "Camila", "Ríos", "camila.rios@mail.com", "2804-445566", 1994),
                    EstadoAsistencia.AUSENTE, null);
        });

        // ---------- 3) En 20 días, varios días con refugio ----------
        Optional.ofNullable(excursiones.get("Travesía Cerro Alto — Río Senguer")).ifPresent(exc -> {
            Salida s = salida(exc, hoy.plusDays(20), hoy.plusDays(22), LocalTime.of(8, 0), LocalTime.of(18, 0),
                    8, 20, 14, 2, EstadoSalida.DISPONIBLE);
            asignar(almeida, s, TipoGuia.RESPONSABLE);
            asignar(coronel, s, TipoGuia.AYUDANTE);
            AsignacionRefugio ar = new AsignacionRefugio(refugioCerroAlto, s, 1, 10);
            ar.setPlazasOcupadas(2);
            asignacionRefugioRepository.save(ar);

            Reserva f = reserva(s, cliente("37112233", "Lucía", "Fernández", "lucia.fernandez@mail.com", "11-4455-6677"),
                    EstadoReserva.CONFIRMADA, 2, "420000", "0");
            f.setModalidadAlojamiento(ModalidadAlojamiento.REFUGIO);
            reservaRepository.save(f);
            certificado(participante(f, "37112233", "Lucía", "Fernández", "lucia.fernandez@mail.com", "11-4455-6677", 1993),
                    EstadoCertificado.APROBADO);
            participante(f, "39887766", "Diego", "Gómez", "diego.gomez@mail.com", "11-6677-8899", 1995);
            pedirEquipamiento(equipo.get("Bastones de trekking"), f, 2);
        });

        // ---------- 4) En 10 días, Martín como guía AYUDANTE (no puede finalizarla) ----------
        Optional.ofNullable(excursiones.get("Circuito Lagos Patagónicos")).ifPresent(exc -> {
            Salida s = salida(exc, hoy.plusDays(10), hoy.plusDays(14), LocalTime.of(7, 30), LocalTime.of(18, 0),
                    6, 14, 14, 2, EstadoSalida.DISPONIBLE);
            asignar(ibarra, s, TipoGuia.RESPONSABLE);
            asignar(almeida, s, TipoGuia.AYUDANTE);

            Reserva h = reserva(s, cliente("30554433", "Pablo", "Herrera", "pablo.herrera@mail.com", "11-2211-3344"),
                    EstadoReserva.SENADA, 2, "620000", "310000");
            certificado(participante(h, "30554433", "Pablo", "Herrera", "pablo.herrera@mail.com", "11-2211-3344", 1985),
                    EstadoCertificado.APROBADO);
            certificado(participante(h, "42112299", "Sofía", "Luna", "sofia.luna@mail.com", "11-9988-7766", 2000),
                    EstadoCertificado.RECHAZADO);
        });

        // ---------- 5) Hace 12 días, Finalizada con observaciones ----------
        Optional.ofNullable(excursiones.get("Ascenso Cerro Perito Moreno")).ifPresent(exc -> {
            LocalDate dia = hoy.minusDays(12);
            Salida s = salida(exc, dia, dia, LocalTime.of(8, 0), LocalTime.of(17, 0), 8, 14, 14, 2, EstadoSalida.FINALIZADA);
            s.setFechaHoraFinReal(dia.atTime(16, 45));
            s.setObservaciones("La salida se completó sin incidentes. Se acortó el último tramo por viento fuerte en el filo.");
            salidaRepository.save(s);
            asignar(almeida, s, TipoGuia.RESPONSABLE);

            Reserva i = reserva(s, cliente("34221100", "Elena", "Castro", "elena.castro@mail.com", "11-3322-1100"),
                    EstadoReserva.CONFIRMADA, 2, "190000", "0");
            Participante castro = participante(i, "34221100", "Elena", "Castro", "elena.castro@mail.com", "11-3322-1100", 1989);
            certificado(castro, EstadoCertificado.APROBADO);
            acreditar(castro, EstadoAsistencia.PRESENTE, EstadoRequisitos.APTO);
            Participante benitez = participante(i, "36554411", "Ramiro", "Benítez", "ramiro.benitez@mail.com", "11-4411-5522", 1992);
            certificado(benitez, EstadoCertificado.APROBADO);
            acreditar(benitez, EstadoAsistencia.PRESENTE, EstadoRequisitos.NO_APTO);
        });
    }

    // ---------------- auxiliares ----------------

    private Salida salida(Excursion excursion, LocalDate inicio, LocalDate fin, LocalTime horaInicio, LocalTime horaFin,
                          int cupoBase, int cupoMaximo, int cupoTotal, int cuposOcupados, EstadoSalida estado) {
        Salida s = new Salida();
        s.setExcursion(excursion);
        s.setFechaInicio(inicio);
        s.setFechaFinalizacion(fin);
        s.setHoraInicio(horaInicio);
        s.setHoraFinalizacion(horaFin);
        s.setCupoBase(cupoBase);
        s.setCupoMaximo(cupoMaximo);
        s.setCupoTotal(cupoTotal);
        s.setCuposOcupados(cuposOcupados);
        s.setEstadoSalida(estado);
        s.setRecorrido(excursion.getRecorridoBase());
        return salidaRepository.save(s);
    }

    private void asignar(Guia guia, Salida salida, TipoGuia tipo) {
        asignacionGuiaRepository.save(new AsignacionGuia(guia, salida, tipo));
    }

    private Cliente cliente(String dni, String nombre, String apellido, String correo, String telefono) {
        return clienteRepository.save(new Cliente(dni, nombre, apellido, correo, telefono, LocalDate.of(1990, 1, 1)));
    }

    private Reserva reserva(Salida salida, Cliente cliente, EstadoReserva estado, int cantidad, String monto, String saldo) {
        Reserva r = new Reserva();
        r.setSalida(salida);
        r.setCliente(cliente);
        r.setFechaHora(LocalDateTime.now().minusDays(15));
        r.setEstadoReserva(estado);
        r.setCantidadParticipantes(cantidad);
        r.setMontoTotal(new BigDecimal(monto));
        r.setSaldoPendiente(new BigDecimal(saldo));
        return reservaRepository.save(r);
    }

    private Participante participante(Reserva reserva, String dni, String nombre, String apellido, String correo,
                                      String telefono, int anioNacimiento) {
        return participanteRepository.save(new Participante(reserva, dni, nombre, apellido, correo, telefono,
                LocalDate.of(anioNacimiento, 6, 15)));
    }

    private void acreditar(Participante p, EstadoAsistencia asistencia, EstadoRequisitos requisitos) {
        p.setEstadoAsistencia(asistencia);
        p.setEstadoRequisitos(requisitos);
        participanteRepository.save(p);
    }

    private void certificado(Participante p, EstadoCertificado estado) {
        CertificadoMedico c = new CertificadoMedico(p, "certificados/demo-" + p.getDni() + ".pdf",
                LocalDateTime.now().minusDays(10), LocalDate.now().plusMonths(6), estado);
        if (estado == EstadoCertificado.RECHAZADO) {
            c.setMotivoRechazo("El certificado no tiene firma ni sello del médico.");
        }
        certificadoRepository.save(c);
    }

    private void pedirEquipamiento(Equipamiento item, Reserva reserva, int cantidad) {
        if (item == null) return; // el ítem no existe en el catálogo
        asignacionEquipamientoRepository.save(
                new AsignacionEquipamiento(item, reserva, cantidad, EstadoAsignacion.CONSOLIDADO));
    }
}
