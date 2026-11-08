package com.patatrek.backend.config;

import com.patatrek.backend.certificado.CertificadoMedico;
import com.patatrek.backend.certificado.CertificadoMedicoRepository;
import com.patatrek.backend.certificado.EstadoCertificado;
import com.patatrek.backend.contingencia.Contingencia;
import com.patatrek.backend.contingencia.ContingenciaRepository;
import com.patatrek.backend.equipamiento.Equipamiento;
import com.patatrek.backend.equipamiento.EquipamientoRepository;
import com.patatrek.backend.excursion.Dificultad;
import com.patatrek.backend.excursion.EstadoExcursion;
import com.patatrek.backend.excursion.Excursion;
import com.patatrek.backend.excursion.ExcursionRepository;
import com.patatrek.backend.pago.EstadoPago;
import com.patatrek.backend.pago.ModoPago;
import com.patatrek.backend.pago.Pago;
import com.patatrek.backend.pago.PagoRepository;
import com.patatrek.backend.reserva.*;
import com.patatrek.backend.salida.*;
import com.patatrek.backend.usuario.*;
import com.patatrek.backend.valoracion.Valoracion;
import com.patatrek.backend.valoracion.ValoracionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Datos para la entrega de la documentación y la exposición: un catálogo completo de excursiones, guías, clientes,
 * salidas en todos los estados y reservas con sus pagos, certificados, asistencias, valoraciones y contingencias.
 *
 * Las fechas se calculan desde el día en que se cargan ("hoy"), así que siempre quedan coherentes:
 * salidas finalizadas en las semanas anteriores, salidas de hoy (una lista para el control de asistencia y otra en
 * curso), salidas dentro de las 72 horas (para la cancelación automática) y salidas futuras para reservar.
 * Para la exposición conviene cargarlos el mismo día (ver DATOS-EXPOSICION.md).
 *
 * Solo se cargan con patatrek.cargar-datos-exposicion=true y si la base está vacía. No reemplazan a los datos de
 * ejemplo que usan las pruebas automáticas (perfil demo).
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "patatrek.cargar-datos-exposicion", havingValue = "true")
public class DatosExposicion implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosExposicion.class);
    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Set<EstadoReserva> OCUPAN = EnumSet.of(EstadoReserva.SENADA, EstadoReserva.CONFIRMADA);

    private final AdministradorRepository administradorRepository;
    private final GuiaRepository guiaRepository;
    private final ClienteRepository clienteRepository;
    private final EquipamientoRepository equipamientoRepository;
    private final ExcursionRepository excursionRepository;
    private final RefugioRepository refugioRepository;
    private final SalidaRepository salidaRepository;
    private final AsignacionGuiaRepository asignacionGuiaRepository;
    private final AsignacionRefugioRepository asignacionRefugioRepository;
    private final ReservaRepository reservaRepository;
    private final ParticipanteRepository participanteRepository;
    private final AsignacionEquipamientoRepository asignacionEquipamientoRepository;
    private final EntregaEquipamientoRepository entregaEquipamientoRepository;
    private final PagoRepository pagoRepository;
    private final CertificadoMedicoRepository certificadoRepository;
    private final ContingenciaRepository contingenciaRepository;
    private final ValoracionRepository valoracionRepository;
    private final PasswordEncoder passwordEncoder;
    private final String contrasenia;
    private final Path carpetaArchivos;

    // Estado de la carga
    private LocalDate hoy;
    private LocalDateTime ahora;
    private String hash;
    private final Map<String, Equipamiento> equipo = new HashMap<>();
    private final Map<Long, List<AsignacionRefugio>> refugiosPorSalida = new HashMap<>();
    private final Map<Long, List<Guia>> guiasPorSalida = new HashMap<>();
    private final List<Salida> salidas = new ArrayList<>();
    private final Random azar = new Random(2026);
    private int siguienteDni = 0;
    private final Map<String, Persona> personas = new HashMap<>();
    private final Set<String> dnisUsados = new HashSet<>();
    private int siguienteTransaccion = 0;

    public DatosExposicion(AdministradorRepository administradorRepository, GuiaRepository guiaRepository,
                           ClienteRepository clienteRepository, EquipamientoRepository equipamientoRepository,
                           ExcursionRepository excursionRepository, RefugioRepository refugioRepository,
                           SalidaRepository salidaRepository, AsignacionGuiaRepository asignacionGuiaRepository,
                           AsignacionRefugioRepository asignacionRefugioRepository, ReservaRepository reservaRepository,
                           ParticipanteRepository participanteRepository,
                           AsignacionEquipamientoRepository asignacionEquipamientoRepository,
                           EntregaEquipamientoRepository entregaEquipamientoRepository, PagoRepository pagoRepository,
                           CertificadoMedicoRepository certificadoRepository,
                           ContingenciaRepository contingenciaRepository, ValoracionRepository valoracionRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${patatrek.contrasenia-demo:Patatrek2026}") String contrasenia,
                           @Value("${patatrek.archivos.directorio:uploads}") String carpetaArchivos) {
        this.administradorRepository = administradorRepository;
        this.guiaRepository = guiaRepository;
        this.clienteRepository = clienteRepository;
        this.equipamientoRepository = equipamientoRepository;
        this.excursionRepository = excursionRepository;
        this.refugioRepository = refugioRepository;
        this.salidaRepository = salidaRepository;
        this.asignacionGuiaRepository = asignacionGuiaRepository;
        this.asignacionRefugioRepository = asignacionRefugioRepository;
        this.reservaRepository = reservaRepository;
        this.participanteRepository = participanteRepository;
        this.asignacionEquipamientoRepository = asignacionEquipamientoRepository;
        this.entregaEquipamientoRepository = entregaEquipamientoRepository;
        this.pagoRepository = pagoRepository;
        this.certificadoRepository = certificadoRepository;
        this.contingenciaRepository = contingenciaRepository;
        this.valoracionRepository = valoracionRepository;
        this.passwordEncoder = passwordEncoder;
        this.contrasenia = contrasenia;
        this.carpetaArchivos = Path.of(carpetaArchivos).toAbsolutePath().normalize();
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (excursionRepository.count() > 0 || administradorRepository.count() > 0) {
            log.info("Datos de la exposición: la base ya tiene datos, no se cargan.");
            return;
        }
        hoy = LocalDate.now();
        ahora = LocalDateTime.now();
        hash = passwordEncoder.encode(contrasenia);

        cargarCatalogo();
        Map<String, Excursion> exc = new HashMap<>();
        excursionRepository.findAll().forEach(e -> exc.put(e.getNombre(), e));
        Excursion travesia = exc.get(TRAVESIA), lagos = exc.get(LAGOS), perito = exc.get(PERITO),
                laguna = exc.get(LAGUNA), arrayanes = exc.get(ARRAYANES), glaciar = exc.get(GLACIAR);

        // ---------------- Usuarios ----------------
        dnisUsados.addAll(List.of("25111333", "27456890"));
        administradorRepository.save(new Administrador("25111333", "Valeria", "Suárez", "admin@patatrek.com", hash, "2804-500100"));
        administradorRepository.save(new Administrador("27456890", "Hernán", "Gallardo", "hernan.gallardo@patatrek.com", hash, "2804-500101"));

        Guia almeida = guia("28456123", "Martín", "Almeida", "2804-551122", 8);
        Guia coronel = guia("31877654", "Brenda", "Coronel", "2804-553344", 6);
        Guia ibarra = guia("29333222", "Lucas", "Ibarra", "2804-556677", 6);
        Guia ortiz = guia("30998214", "Valentina", "Ortiz", "2804-558811", 8);
        Guia nancucheo = guia("27665432", "Federico", "Ñancucheo", "2804-559023", 10);
        Guia huenchul = guia("33012876", "Carla", "Huenchul", "2804-554409", 6);
        Guia rivas = guia("32456781", "Joaquín", "Rivas", "2804-552918", 8);

        Cliente sofia = cliente("38555777", "Sofía", "Ibarra", "2804-667788", 1994);
        Cliente rocio = cliente("36108442", "Rocío", "Delgado", "11-3344-5566", 1991);
        Cliente bruno = cliente("41902334", "Bruno", "Molina", "11-5566-2211", 1999);
        Cliente julieta = cliente("33445566", "Julieta", "Sosa", "2804-112233", 1988);
        Cliente lucia = cliente("37112233", "Lucía", "Fernández", "11-4455-6677", 1993);
        Cliente pablo = cliente("30554433", "Pablo", "Herrera", "11-2211-3344", 1985);
        Cliente elena = cliente("34221100", "Elena", "Castro", "11-3322-1100", 1989);
        Cliente marcos = cliente("33221144", "Marcos", "Quiroga", "11-6655-4433", 1988);
        Cliente julian = cliente("36444555", "Julián", "Ferreyra", "2804-551199", 1991);
        Cliente melina = cliente("40222333", "Melina", "Cabral", "2804-553300", 1997);
        Cliente paula = cliente("41555666", "Paula", "Medina", "2804-559900", 1999);
        Cliente diego = cliente("32111999", "Diego", "Sosa", "2804-557788", 1985);
        Cliente agustin = cliente("39887001", "Agustín", "Barrios", "297-415-2231", 1996);
        Cliente florencia = cliente("35112874", "Florencia", "Navarro", "297-418-7712", 1990);
        Cliente gonzalo = cliente("31678902", "Gonzalo", "Peralta", "11-4789-3321", 1984);
        Cliente micaela = cliente("42330119", "Micaela", "Torres", "2804-660231", 2000);
        Cliente ignacio = cliente("38001457", "Ignacio", "Luna", "2945-44-1189", 1994);
        Cliente carolina = cliente("36790345", "Carolina", "Benítez", "11-5032-8876", 1992);
        Cliente hernan = cliente("29884510", "Hernán", "Aguirre", "2804-447712", 1982);
        Cliente antonella = cliente("43112908", "Antonella", "Ruiz", "297-433-1904", 2001);
        Cliente tomas = cliente("40566781", "Tomás", "Villalba", "11-6123-4490", 1997);
        Cliente daniela = cliente("34887123", "Daniela", "Correa", "2804-331245", 1989);
        Cliente emiliano = cliente("37554210", "Emiliano", "Godoy", "297-422-6650", 1993);
        Cliente victoria = cliente("41008765", "Victoria", "Lagos", "2945-45-7781", 1998);
        Cliente ramiro = cliente("33990876", "Ramiro", "Acuña", "11-4567-0021", 1987);
        Cliente jimena = cliente("39220458", "Jimena", "Paredes", "2804-668812", 1996);

        // Acompañante de Sofía con datos fijos: se usa en la exposición para entrar como participante (N.º de reserva + DNI)
        personas.put("Mateo Ibarra", new Persona("45111222", "Mateo", "Ibarra", "mateo.ibarra@mail.com", "2804-667790",
                LocalDate.of(1998, 5, 14)));
        dnisUsados.add("45111222");

        Refugio cerroAlto = refugioRepository.save(new Refugio("Refugio Cerro Alto", 30, new BigDecimal("25000")));
        Refugio valleEscondido = refugioRepository.save(new Refugio("Refugio Valle Escondido", 20, new BigDecimal("22000")));
        Refugio coihues = refugioRepository.save(new Refugio("Refugio Los Coihues", 24, new BigDecimal("28000")));
        Refugio laPlata = refugioRepository.save(new Refugio("Refugio Lago La Plata", 16, new BigDecimal("26000")));
        Refugio torrecillas = refugioRepository.save(new Refugio("Refugio Torrecillas", 12, new BigDecimal("24000")));

        // =====================================================================
        //  Salidas finalizadas (CU-16, CU-17, CU-28, CU-29)
        // =====================================================================
        Salida p1 = salida(laguna, -41, h(9), h(14), 14, ortiz);
        Salida p2 = salida(perito, -34, h(8), h(17), 14, almeida, coronel);
        Salida p3 = salida(travesia, -29, h(8), h(18), 20, nancucheo, ibarra);
        refugio(p3, valleEscondido, 1, 10);
        refugio(p3, cerroAlto, 2, 10);
        Salida p4 = salida(arrayanes, -22, h(10), hm(15, 30), 10, huenchul);
        Salida p8 = salida(lagos, -26, hm(7, 30), h(18), 14, almeida);
        refugio(p8, coihues, 1, 10);
        refugio(p8, laPlata, 3, 10);
        Salida p5 = salida(glaciar, -16, h(8), h(17), 12, rivas);
        refugio(p5, torrecillas, 1, 8);
        Salida p6 = salida(laguna, -12, h(9), h(14), 14, ortiz, huenchul);
        Salida p7 = salida(perito, -8, h(8), h(17), 10, ibarra);
        Salida p9 = salida(laguna, -4, hm(9, 30), hm(14, 30), 10, coronel);
        Salida p10 = salida(arrayanes, -2, h(10), hm(15, 30), 10, huenchul);

        // P1 Laguna
        finalizada(reserva(p1, marcos).dias(30).total().con(menor("Lara", "Quiroga", 2013), adulto("Paola", "Ríos", 1989)).guardar(),
                5, "Excelente organización y el guía muy atento. Ideal para ir en familia.");
        finalizada(reserva(p1, daniela).dias(22).total().con(adulto("Sergio", "Correa", 1987)).guardar(),
                4, "Muy lindo el recorrido, la laguna es hermosa. Faltó un poco más de tiempo para fotos.");
        finalizada(reserva(p1, carolina).dias(18).enDosPagos().con(menor("Benjamín", "Benítez", 2014), menor("Olivia", "Benítez", 2016)).guardar(),
                5, "Los chicos lo disfrutaron muchísimo. El sendero está muy bien señalizado.");
        // P2 Perito Moreno
        finalizada(reserva(p2, elena).dias(25).total().equipo("Casco", 2).con(adulto("Ramiro", "Benítez", 1992)).guardar(),
                4, "Exigente pero vale cada paso. La vista desde la cumbre es impresionante.");
        Reserva p2b = reserva(p2, gonzalo).dias(20).enDosPagos().equipo("Casco", 1).equipo("Guantes térmicos", 1)
                .con(adulto("Nicolás", "Peralta", 1986)).guardar();
        finalizada(p2b, 5, "Brenda y Martín son unos genios, se nota la experiencia. Volvería sin dudarlo.");
        acreditar(p2b, 1, EstadoAsistencia.PRESENTE, EstadoRequisitos.NO_APTO); // no presentó el casco
        Reserva p2c = reserva(p2, tomas).dias(14).total().guardar();
        finalizada(p2c, 3, "La subida es durísima. Recomiendo ir con muy buen estado físico.");
        // P3 Travesía con refugios
        finalizada(reserva(p3, lucia).dias(40).total().refugio().equipo("Bastones de trekking", 2)
                .con(adulto("Diego", "Gómez", 1995)).guardar(), 5,
                "Tres días inolvidables. Los refugios cómodos y la comida muy rica.");
        finalizada(reserva(p3, ignacio).dias(33).enDosPagos().con(adulto("Martina", "Luna", 1996), adulto("Federico", "Luna", 1991)).guardar(),
                4, "Hermoso paisaje. El segundo día fue largo pero los guías marcan muy bien el ritmo.");
        finalizada(reserva(p3, hernan).dias(28).total().refugio().equipo("Bolsa de dormir", 1).guardar(),
                5, "La mejor travesía que hice en la Patagonia.");
        // P4 Arrayanes
        finalizada(reserva(p4, victoria).dias(12).total().con(adulto("Agustina", "Lagos", 1999)).guardar(),
                5, "Un bosque de cuento y la cascada increíble. Carla explica todo con mucho detalle.");
        finalizada(reserva(p4, jimena).dias(9).total().con(menor("Thiago", "Paredes", 2015)).guardar(),
                4, "Paseo tranquilo, ideal para hacer con chicos.");
        // P8 Lagos (5 días)
        finalizada(reserva(p8, pablo).dias(45).enDosPagos().refugio().equipo("Bolsa de dormir", 2).equipo("Colchoneta aislante", 2)
                .con(adulto("Sofía", "Luna", 2000)).guardar(), 5, "Cinco días que valen cada kilómetro. Organización impecable.");
        finalizada(reserva(p8, ramiro).dias(38).total().refugio().con(adulto("Laura", "Acuña", 1989), adulto("Iván", "Acuña", 1985)).guardar(),
                4, "Muy exigente. Los refugios están bien equipados.");
        // P5 Glaciar
        finalizada(reserva(p5, emiliano).dias(20).total().refugio().equipo("Bastones de trekking", 1)
                .con(adulto("Rocío", "Godoy", 1995)).guardar(), 5, "Ver el glaciar de cerca no tiene precio.");
        Reserva p5b = reserva(p5, florencia).dias(17).total().guardar();
        acreditar(p5b, 0, EstadoAsistencia.AUSENTE, null);
        // P6 Laguna
        finalizada(reserva(p6, antonella).dias(11).total().con(adulto("Camila", "Ruiz", 2002), menor("Joaquín", "Ruiz", 2012)).guardar(),
                4, "Muy recomendable para principiantes.");
        finalizada(reserva(p6, micaela).dias(7).total().con(adulto("Lautaro", "Torres", 1999)).guardar(), 0, null);
        // P7 Perito Moreno
        finalizada(reserva(p7, agustin).dias(16).total().equipo("Casco", 2).con(adulto("Gabriel", "Barrios", 1994)).guardar(),
                2, "El día estuvo muy nublado y no se vio nada desde la cumbre. Igual el guía fue muy amable.");
        // P9 Laguna: Sofía todavía no valoró (CU-28 desde su panel)
        finalizada(reserva(p9, sofia).dias(10).total().con(adulto("Mateo", "Ibarra", 1998)).guardar(), 0, null);
        finalizada(reserva(p9, rocio).dias(6).total().guardar(), 5, "Hermoso día, súper recomendable.");
        // P10 Arrayanes: valoraciones pendientes
        finalizada(reserva(p10, julieta).dias(8).total().con(menor("Mateo", "Sosa", 2015)).guardar(), 0, null);
        finalizada(reserva(p10, tomas).dias(5).total().guardar(), 0, null);

        cerrar(p1, -41, h(14), "Día soleado, el grupo completó el circuito sin inconvenientes.");
        cerrar(p2, -34, hm(16, 45), "Se acortó el último tramo por viento fuerte en el filo. Un participante no presentó casco y no subió a la cumbre.");
        cerrar(p3, -27, hm(17, 40), "Travesía completa. Crecida leve del río el segundo día, se cruzó por el vado alternativo.");
        cerrar(p4, -22, h(15), "");
        cerrar(p8, -22, hm(17, 30), "Sin incidentes. Se recomienda revisar la bomba de agua del Refugio Lago La Plata.");
        cerrar(p5, -15, hm(16, 20), "Un participante ausente. Buen clima durante los dos días.");
        cerrar(p6, -12, hm(13, 50), "Grupo numeroso con varios chicos; se hicieron paradas extra.");
        cerrar(p7, -8, hm(16, 10), "Nubosidad baja en la cumbre, sin visibilidad.");
        cerrar(p9, -4, hm(14, 20), "Todo en orden.");
        cerrar(p10, -2, h(15), "La pasarela de la cascada está en reparación: se usó el mirador lateral.");

        // =====================================================================
        //  Salidas canceladas (contingencias y baja)
        // =====================================================================
        // Contingencia operativa resuelta hace días: reservas ya devueltas (CU-27 completo)
        Salida x1 = salida(arrayanes, -10, h(10), hm(15, 30), 10, huenchul);
        Reserva x1a = reserva(x1, daniela).dias(20).total().con(adulto("Sergio", "Correa", 1987)).guardar();
        Reserva x1b = reserva(x1, gonzalo).dias(15).sena().guardar();
        cancelarPorContingencia(x1, "OPERATIVA", "Cierre del sendero por incendio forestal en la zona del lago.", -13);
        devolver(x1a, -11);
        devolver(x1b, -11);

        // Contingencia climática reciente: reservas pendientes de devolución (CU-27 para mostrar)
        Salida x2 = salida(perito, 5, h(8), h(17), 14, almeida, coronel);
        reserva(x2, julian).dias(18).sena().con(adulto("Carla", "Ferreyra", 1993)).guardar();
        reserva(x2, melina).dias(12).total().guardar();
        reserva(x2, bruno).dias(9).total().equipo("Casco", 1).con(adulto("Agustina", "Paredes", 1990)).guardar();
        Reserva x2d = reserva(x2, paula).dias(20).total().con(adulto("Juan", "Medina", 1997)).guardar();
        cancelarPorContingencia(x2, "CLIMATICA", "Alerta por vientos fuertes en la zona de cumbre.", -2);
        devolver(x2d, -1);

        // Baja administrativa (CU-13), sin reservas
        Salida x3 = salida(laguna, 25, h(9), h(14), 14, ortiz);
        x3.setEstadoSalida(EstadoSalida.CANCELADA);
        x3.setMotivoCancelacion("Baja del " + ahora.minusDays(1).withHour(10).withMinute(15).format(DIA_HORA)
                + ". Motivo: Superposición con el mantenimiento anual del parque.");
        liberarGuiasYRefugios(x3);

        // Una reserva cancelada por el propio cliente (CU-23, sin devolución)
        // y una cancelada por la cancelación automática (CU-30) en una salida ya finalizada
        Reserva cancelada = reserva(p6, sofia).dias(15).sena().guardar();
        cancelarPorCliente(cancelada, "Cambio de planes por trabajo.", -14);
        Reserva cu30 = reserva(p7, carolina).dias(20).sena().con(adulto("Pedro", "Benítez", 1990)).guardar();
        cancelarPorCliente(cu30, "Cancelación automática: no se completó el pago del saldo restante "
                + "antes de las 72 horas previas a la salida. No corresponde devolución de lo abonado.", -11);

        // =====================================================================
        //  Hoy
        // =====================================================================
        // Lista para el CONTROL DE ASISTENCIA (CU-15)
        Salida t1 = salida(perito, 0, h(8), h(17), 14, almeida, coronel);
        t1.setEstadoSalida(EstadoSalida.DISPONIBLE);
        reserva(t1, rocio).dias(20).total().certificados(EstadoCertificado.APROBADO).equipo("Casco", 1)
                .con(adulto("Tomás", "Vera", 1997)).guardar();
        reserva(t1, bruno).dias(16).enDosPagos().certificados(EstadoCertificado.CARGADO, EstadoCertificado.PENDIENTE)
                .con(adulto("Agustina", "Paredes", 1990)).guardar();
        reserva(t1, florencia).dias(12).total().certificados(EstadoCertificado.APROBADO).equipo("Casco", 2).equipo("Guantes térmicos", 3)
                .con(adulto("Martín", "Navarro", 1988), adulto("Sol", "Navarro", 1995)).guardar();

        // EN CURSO, control hecho: lista para MARCAR FINALIZACIÓN (CU-16)
        Salida t2 = salida(laguna, 0, hm(9, 30), hm(14, 30), 12, ortiz);
        t2.setEstadoSalida(EstadoSalida.EN_CURSO);
        Reserva t2a = reserva(t2, julieta).dias(9).total().con(menor("Mateo", "Sosa", 2015)).guardar();
        acreditarTodos(t2a);
        Reserva t2b = reserva(t2, victoria).dias(6).total().con(adulto("Agustina", "Lagos", 1999), adulto("Bruno", "Lagos", 1996)).guardar();
        acreditarTodos(t2b);
        acreditar(t2b, 2, EstadoAsistencia.AUSENTE, null);

        // Travesía EN CURSO desde ayer (varios días, con refugios)
        Salida t3 = salida(travesia, -1, h(8), h(18), 20, nancucheo, rivas);
        refugio(t3, valleEscondido, 1, 12);
        refugio(t3, cerroAlto, 2, 12);
        Reserva t3a = reserva(t3, hernan).dias(30).total().refugio().equipo("Bastones de trekking", 2).equipo("Bolsa de dormir", 1)
                .con(adulto("Paula", "Aguirre", 1985)).guardar();
        acreditarTodos(t3a);
        Reserva t3b = reserva(t3, emiliano).dias(25).enDosPagos().con(adulto("Rocío", "Godoy", 1995), adulto("Franco", "Godoy", 1992)).guardar();
        acreditarTodos(t3b);
        Reserva t3c = reserva(t3, micaela).dias(19).total().refugio().guardar();
        acreditarTodos(t3c);

        // =====================================================================
        //  Dentro de las 72 horas: para la CANCELACIÓN AUTOMÁTICA (CU-30)
        // =====================================================================
        Salida k1 = salida(arrayanes, 2, h(10), hm(15, 30), 10, huenchul);
        reserva(k1, antonella).dias(15).sena().con(adulto("Camila", "Ruiz", 2002)).guardar();    // adeuda el saldo → se cancela
        reserva(k1, ramiro).dias(11).total().con(adulto("Laura", "Acuña", 1989)).guardar();      // al día → sigue

        Salida k2 = salida(glaciar, 2, h(8), h(17), 12, ibarra);
        refugio(k2, torrecillas, 1, 8);
        reserva(k2, agustin).dias(14).total().certificados(EstadoCertificado.PENDIENTE, EstadoCertificado.PENDIENTE)
                .con(adulto("Gabriel", "Barrios", 1994)).guardar();                                  // sin certificados → se cancela
        reserva(k2, lucia).dias(12).total().refugio().certificados(EstadoCertificado.APROBADO, EstadoCertificado.PENDIENTE)
                .con(adulto("Diego", "Gómez", 1995)).guardar();                                       // uno aprobado → sigue

        // =====================================================================
        //  Próximas salidas
        // =====================================================================
        // Completa (CU-12: el cupo se libera agregando un ayudante)
        Salida u1 = salida(laguna, 6, h(9), h(14), 10, coronel);
        reserva(u1, marcos).dias(12).total().con(menor("Lara", "Quiroga", 2013), adulto("Paola", "Ríos", 1989)).guardar();
        reserva(u1, carolina).dias(8).enDosPagos().con(menor("Benjamín", "Benítez", 2014)).guardar();
        reserva(u1, ignacio).dias(4).total().guardar();

        // Reprogramada por contingencia: los titulares tienen que aceptar o rechazar (Sofía incluida)
        Salida u2 = salida(glaciar, 9, h(8), h(17), 12, rivas);
        refugio(u2, torrecillas, 1, 8);
        Reserva u2a = reserva(u2, sofia).dias(16).total().refugio().certificados(EstadoCertificado.APROBADO, EstadoCertificado.CARGADO)
                .con(adulto("Mateo", "Ibarra", 1998)).guardar();
        Reserva u2b = reserva(u2, daniela).dias(13).sena().certificados(EstadoCertificado.APROBADO).guardar();
        Reserva u2c = reserva(u2, tomas).dias(10).total().certificados(EstadoCertificado.CARGADO).guardar();
        reprogramar(u2, 7, "CLIMATICA", "Pronóstico de nevadas intensas en el valle para la fecha original.", -3,
                List.of(u2a, u2b), List.of(u2c));

        // Travesía con refugios: certificados para validar (CU-25), reserva señada de Sofía (pago del saldo por el
        // participante, CU-19 / CU-26) y equipamiento prestado
        Salida u3 = salida(travesia, 12, h(8), h(18), 20, almeida, ortiz, ibarra);
        refugio(u3, valleEscondido, 1, 14);
        refugio(u3, cerroAlto, 2, 14);
        reserva(u3, sofia).dias(9).sena().refugio().certificados(EstadoCertificado.CARGADO, EstadoCertificado.PENDIENTE)
                .equipo("Bastones de trekking", 1).equipo("Bolsa de dormir", 2)
                .con(adulto("Mateo", "Ibarra", 1998)).guardar();
        reserva(u3, pablo).dias(20).total().refugio().certificados(EstadoCertificado.APROBADO, EstadoCertificado.RECHAZADO)
                .equipo("Campera cortavientos", 1).con(adulto("Sofía", "Luna", 2000)).guardar();
        reserva(u3, lucia).dias(17).enDosPagos().refugio().certificados(EstadoCertificado.CARGADO, EstadoCertificado.CARGADO)
                .con(adulto("Diego", "Gómez", 1995)).guardar();
        reserva(u3, gonzalo).dias(11).sena().certificados(EstadoCertificado.CARGADO, EstadoCertificado.CARGADO)
                .equipo("Bastones de trekking", 2).con(adulto("Nicolás", "Peralta", 1986)).guardar();
        reserva(u3, jimena).dias(6).total().refugio().certificados(EstadoCertificado.CARGADO).guardar();
        reserva(u3, elena).dias(5).rechazoPrevio().total().certificados(EstadoCertificado.APROBADO, EstadoCertificado.CARGADO)
                .con(adulto("Ramiro", "Benítez", 1992)).guardar();

        // Recorrido modificado por contingencia operativa (sin cambio de fecha)
        Salida u4 = salida(arrayanes, 15, h(10), hm(15, 30), 10, huenchul);
        reserva(u4, victoria).dias(7).total().con(adulto("Agustina", "Lagos", 1999)).guardar();
        reserva(u4, florencia).dias(4).sena().con(menor("Lucas", "Navarro", 2016)).guardar();
        modificarRecorrido(u4, "Puente colgante cerrado por mantenimiento.",
                "Estacionamiento Lago Futalaufquen – Sendero sur por la costa – Mirador de la cascada – regreso por el mismo camino.", -1);

        // Sin reservas: para mostrar en vivo Modificar (CU-11), Personal y cupos (CU-12) y Dar de baja (CU-13)
        salida(perito, 19, h(8), h(17), 12, ibarra);

        // Circuito de cinco días con refugios
        Salida u6 = salida(lagos, 21, hm(7, 30), h(18), 16, nancucheo, coronel);
        refugio(u6, coihues, 1, 12);
        refugio(u6, laPlata, 3, 12);
        reserva(u6, ramiro).dias(14).sena().refugio().certificados(EstadoCertificado.CARGADO, EstadoCertificado.PENDIENTE)
                .equipo("Bolsa de dormir", 2).equipo("Linterna frontal", 2).con(adulto("Iván", "Acuña", 1985)).guardar();
        reserva(u6, hernan).dias(10).total().refugio().certificados(EstadoCertificado.APROBADO).guardar();
        reserva(u6, emiliano).dias(6).sena().certificados(EstadoCertificado.PENDIENTE, EstadoCertificado.PENDIENTE)
                .con(adulto("Franco", "Godoy", 1992)).guardar();

        Salida u7 = salida(laguna, 27, h(9), h(14), 14, ortiz);
        reserva(u7, jimena).dias(3).sena().con(menor("Thiago", "Paredes", 2015), adulto("Marcelo", "Paredes", 1990)).guardar();
        reserva(u7, antonella).dias(2).total().guardar();

        Salida u8 = salida(arrayanes, 30, h(10), hm(15, 30), 10, huenchul);
        reserva(u8, micaela).dias(2).total().con(adulto("Lautaro", "Torres", 1999)).guardar();

        Salida u9 = salida(perito, 33, h(8), h(17), 14, almeida);
        reserva(u9, ignacio).dias(1).sena().certificados(EstadoCertificado.PENDIENTE).guardar();

        Salida u10 = salida(travesia, 40, h(8), h(18), 18, rivas, huenchul);
        refugio(u10, valleEscondido, 1, 10);
        refugio(u10, cerroAlto, 2, 10);
        reserva(u10, julian).dias(2).total().refugio().certificados(EstadoCertificado.PENDIENTE, EstadoCertificado.PENDIENTE)
                .con(adulto("Carla", "Ferreyra", 1993)).guardar();

        Salida u11 = salida(glaciar, 47, h(8), h(17), 12, ibarra);
        refugio(u11, torrecillas, 1, 8);

        // ---------------- Cierre ----------------
        for (Salida s : salidas) {
            if (s.getEstadoSalida() == EstadoSalida.DISPONIBLE && s.getCuposOcupados() >= s.getCupoTotal()) {
                s.setEstadoSalida(EstadoSalida.COMPLETA);
            }
        }
        salidaRepository.saveAll(salidas);
        verificar();
        log.info("Datos de la exposición cargados: {} excursiones, {} guías, {} clientes, {} salidas, {} reservas, "
                        + "{} participantes, {} pagos, {} valoraciones.",
                excursionRepository.count(), guiaRepository.count(), clienteRepository.count(), salidaRepository.count(),
                reservaRepository.count(), participanteRepository.count(), pagoRepository.count(),
                valoracionRepository.count());
    }

    // =========================================================================
    //  Catálogo
    // =========================================================================

    private static final String TRAVESIA = "Travesía Cerro Alto — Río Senguer";
    private static final String LAGOS = "Circuito Lagos Patagónicos";
    private static final String PERITO = "Ascenso Cerro Perito Moreno";
    private static final String LAGUNA = "Sendero Laguna del Bosque";
    private static final String ARRAYANES = "Bosque de Arrayanes y Cascada";
    private static final String GLACIAR = "Valle del Glaciar Torrecillas";

    private void cargarCatalogo() {
        List<Equipamiento> items = equipamientoRepository.saveAll(List.of(
                new Equipamiento("Bastones de trekking", "Par de bastones regulables de aluminio", 40),
                new Equipamiento("Campera cortavientos", "Campera impermeable rompeviento, talles S a XL", 25),
                new Equipamiento("Casco", "Casco de montaña certificado", 30),
                new Equipamiento("Guantes térmicos", "Par de guantes para bajas temperaturas", 30),
                new Equipamiento("Linterna frontal", "Linterna LED de cabeza con pilas", 35),
                new Equipamiento("Botella de agua reutilizable", "Botella térmica de 1 litro", 40),
                new Equipamiento("Kit de primeros auxilios personal", "Botiquín individual", 20),
                new Equipamiento("Bolsa de dormir", "Bolsa de dormir de tres estaciones (-5 °C)", 30),
                new Equipamiento("Colchoneta aislante", "Colchoneta térmica enrollable", 25),
                new Equipamiento("Mochila 40 L", "Mochila de trekking de 40 litros", 20),
                new Equipamiento("Polainas", "Polainas impermeables para barro y nieve", 20),
                new Equipamiento("Grampones", "Grampones de 10 puntas para hielo", 15)));
        items.forEach(i -> equipo.put(i.getNombre(), i));

        excursionRepository.saveAll(List.of(
                excursion(TRAVESIA,
                        "Tres días de trekking entre valles y refugios de montaña, con vistas al Río Senguer y al Cerro Alto.",
                        "Terreno mixto de ripio y sendero de montaña. Jornadas de 6 a 7 horas de caminata con mochila. "
                                + "Desnivel acumulado de 1.200 m. Exigencia física media.",
                        "Base Alto Río Senguer – Refugio Valle Escondido – Mirador Cerro Alto – Refugio Cerro Alto – regreso por el río.",
                        Dificultad.MEDIA, 3, "185000", true, false,
                        "Bastones de trekking", "Bolsa de dormir", "Campera cortavientos"),
                excursion(LAGOS,
                        "Una travesía de cinco días conectando lagos glaciares y bosques nativos de la cordillera chubutense.",
                        "Jornadas de 6 a 8 horas de caminata con mochila de 12 kg. Cruces de arroyos y tramos de pendiente "
                                + "pronunciada. Se recomienda experiencia previa en travesías.",
                        "Lago Fontana – Lago La Plata – Paso del Bosque – Refugio Los Coihues – Refugio Lago La Plata – regreso.",
                        Dificultad.ALTA, 5, "310000", true, false,
                        "Bastones de trekking", "Bolsa de dormir", "Colchoneta aislante", "Linterna frontal"),
                excursion(PERITO,
                        "Una jornada exigente de ascenso con vistas panorámicas de la cordillera y los lagos.",
                        "Ascenso de 900 m de desnivel en terreno rocoso, con un tramo final de filo expuesto. "
                                + "Se requiere buena condición física. Salida temprano para evitar el viento de la tarde.",
                        "Base del cerro – Bosque de lengas – Filo norte – Cumbre (1.850 m) – descenso por la misma ruta.",
                        Dificultad.ALTA, 1, "95000", true, false, "Casco", "Guantes térmicos"),
                excursion(LAGUNA,
                        "Caminata de baja exigencia, ideal para familias, entre bosque nativo de lengas y una laguna de agua turquesa.",
                        "Sendero señalizado de 8 km con pendiente suave y sin tramos técnicos. Apto para principiantes y chicos "
                                + "desde los 8 años.",
                        "Portada del parque – Bosque de lengas – Laguna del Bosque – Mirador del valle – regreso circular.",
                        Dificultad.BAJA, 1, "42000", false, true),
                excursion(ARRAYANES,
                        "Paseo por un bosque de arrayanes centenarios hasta una cascada de 30 metros, a orillas del lago.",
                        "Recorrido de 6 km ida y vuelta por pasarelas y sendero de tierra. Desnivel de 150 m. "
                                + "Exigencia física baja.",
                        "Estacionamiento Lago Futalaufquen – Bosque de arrayanes – Puente colgante – Cascada Escondida – regreso.",
                        Dificultad.BAJA, 1, "38000", false, true),
                excursion(GLACIAR,
                        "Dos días de trekking hasta el frente del Glaciar Torrecillas, con noche en refugio de montaña.",
                        "Primera jornada de 7 horas con 800 m de desnivel; segunda jornada de descenso. Morena glaciaria y "
                                + "tramos de piedra suelta. Exigencia física media.",
                        "Puerto Limonao – Valle del Río Torrecillas – Refugio Torrecillas – Mirador del glaciar – regreso.",
                        Dificultad.MEDIA, 2, "160000", true, false,
                        "Bastones de trekking", "Campera cortavientos", "Guantes térmicos"),
                inactiva(excursion("Travesía Río Azul",
                        "Dos días siguiendo el cauce del Río Azul entre cañadones y estepa.",
                        "Terreno de estepa con cruces de río. Exigencia física media.",
                        "Puesto Río Azul – Cañadón Grande – Campamento Las Bardas – regreso.",
                        Dificultad.MEDIA, 2, "150000", true, false),
                        "Discontinuada por cierre del acceso al campamento.")));
    }

    private Excursion excursion(String nombre, String general, String tecnica, String recorrido, Dificultad dificultad,
                                int dias, String precio, boolean certificado, boolean menores, String... items) {
        Excursion e = new Excursion();
        e.setNombre(nombre);
        e.setDescripcionGeneral(general);
        e.setDescripcionTecnica(tecnica);
        e.setRecorridoBase(recorrido);
        e.setDificultad(dificultad);
        e.setDuracion(dias);
        e.setPrecio(new BigDecimal(precio));
        e.setRequiereCertificado(certificado);
        e.setAceptaMenores(menores);
        List<Equipamiento> lista = Arrays.stream(items).map(equipo::get).filter(Objects::nonNull).toList();
        e.setRequiereEquipamiento(!lista.isEmpty());
        e.setEquipamientoObligatorio(new LinkedHashSet<>(lista));
        return e;
    }

    private Excursion inactiva(Excursion e, String motivo) {
        e.setEstadoExcursion(EstadoExcursion.INACTIVA);
        e.setMotivoBaja(motivo);
        return e;
    }

    // =========================================================================
    //  Usuarios
    // =========================================================================

    private Guia guia(String dni, String nombre, String apellido, String telefono, int capacidad) {
        dnisUsados.add(dni);
        Guia g = new Guia(dni, nombre, apellido, correo(nombre, apellido, "patatrek.com"), telefono, capacidad);
        g.setContrasenia(hash);
        return guiaRepository.save(g);
    }

    private Cliente cliente(String dni, String nombre, String apellido, String telefono, int anio) {
        dnisUsados.add(dni);
        Cliente c = new Cliente(dni, nombre, apellido, correo(nombre, apellido, "mail.com"), telefono,
                LocalDate.of(anio, 1 + azar.nextInt(12), 1 + azar.nextInt(28)));
        c.setContrasenia(hash);
        return clienteRepository.save(c);
    }

    /** "Martín Almeida" → martin.almeida@dominio */
    private static String correo(String nombre, String apellido, String dominio) {
        String base = (nombre + "." + apellido).toLowerCase(Locale.ROOT).replace(' ', '.');
        base = Normalizer.normalize(base, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return base + "@" + dominio;
    }

    // =========================================================================
    //  Salidas
    // =========================================================================

    private static LocalTime h(int hora) {
        return LocalTime.of(hora, 0);
    }

    private static LocalTime hm(int hora, int minutos) {
        return LocalTime.of(hora, minutos);
    }

    /**
     * Salida que empieza dentro de "dia" días (negativo: ya pasó) y dura lo que su excursión. El cupo base es la
     * capacidad del responsable y el cupo total suma un 25 % del base por ayudante (RF-10).
     * El estado inicial es Disponible si no empezó y Finalizada si ya terminó (se ajusta después).
     */
    private Salida salida(Excursion e, int dia, LocalTime inicio, LocalTime fin, int cupoMaximo, Guia responsable,
                          Guia... ayudantes) {
        Salida s = new Salida();
        s.setExcursion(e);
        s.setFechaInicio(hoy.plusDays(dia));
        s.setFechaFinalizacion(hoy.plusDays(dia + e.getDuracion() - 1L));
        s.setHoraInicio(inicio);
        s.setHoraFinalizacion(fin);
        int base = responsable.getCapacidadEstandar();
        s.setCupoBase(base);
        s.setCupoMaximo(cupoMaximo);
        s.setCupoTotal(base + (int) Math.floor(base * 0.25 * ayudantes.length));
        s.setCuposOcupados(0);
        s.setRecorrido(e.getRecorridoBase());
        boolean termino = s.getFechaFinalizacion().atTime(fin).isBefore(ahora);
        s.setEstadoSalida(termino ? EstadoSalida.FINALIZADA : EstadoSalida.DISPONIBLE);
        s = salidaRepository.save(s);
        List<Guia> guias = new ArrayList<>();
        asignacionGuiaRepository.save(new AsignacionGuia(responsable, s, TipoGuia.RESPONSABLE));
        guias.add(responsable);
        for (Guia a : ayudantes) {
            asignacionGuiaRepository.save(new AsignacionGuia(a, s, TipoGuia.AYUDANTE));
            guias.add(a);
        }
        guiasPorSalida.put(s.getIdSalida(), guias);
        salidas.add(s);
        return s;
    }

    private void refugio(Salida s, Refugio r, int orden, int plazas) {
        AsignacionRefugio a = asignacionRefugioRepository.save(new AsignacionRefugio(r, s, orden, plazas));
        refugiosPorSalida.computeIfAbsent(s.getIdSalida(), k -> new ArrayList<>()).add(a);
    }

    /** CU-16: el guía responsable marcó la finalización. */
    private void cerrar(Salida s, int diaFin, LocalTime hora, String observaciones) {
        s.setEstadoSalida(EstadoSalida.FINALIZADA);
        s.setFechaHoraFinReal(hoy.plusDays(diaFin).atTime(hora));
        s.setObservaciones(observaciones == null || observaciones.isBlank() ? null : observaciones);
    }

    private void liberarGuiasYRefugios(Salida s) {
        asignacionGuiaRepository.deleteAll(asignacionGuiaRepository.findBySalidaIdSalidaIn(List.of(s.getIdSalida())));
        guiasPorSalida.remove(s.getIdSalida());
        asignacionRefugioRepository.deleteAll(refugiosPorSalida.getOrDefault(s.getIdSalida(), List.of()));
        refugiosPorSalida.remove(s.getIdSalida());
    }

    // =========================================================================
    //  Reservas
    // =========================================================================

    private record Persona(String dni, String nombre, String apellido, String correo, String telefono, LocalDate nacimiento) {}

    /** Acompañante adulto. La misma persona en varias reservas conserva su DNI y sus datos. */
    private Persona adulto(String nombre, String apellido, int anio) {
        return personas.computeIfAbsent(nombre + " " + apellido, k -> new Persona(dniNuevo(anio), nombre, apellido,
                correo(nombre, apellido, "mail.com"),
                "2804-" + (600000 + azar.nextInt(99999)), LocalDate.of(anio, 1 + azar.nextInt(12), 1 + azar.nextInt(28))));
    }

    /** Menor de edad (solo en excursiones que los admiten): usa el correo del titular de la reserva. */
    private Persona menor(String nombre, String apellido, int anio) {
        return personas.computeIfAbsent(nombre + " " + apellido, k -> new Persona(dniNuevo(anio), nombre, apellido, null,
                "2804-" + (600000 + azar.nextInt(99999)), LocalDate.of(anio, 1 + azar.nextInt(12), 1 + azar.nextInt(28))));
    }

    /** DNI verosímil para el año de nacimiento, que no repite el de otra persona ni el de un usuario. */
    private String dniNuevo(int anio) {
        int base = anio >= 2010 ? 50_000_000 : anio >= 2000 ? 44_000_000 : anio >= 1990 ? 36_000_000 : 29_000_000;
        String dni;
        do {
            dni = String.valueOf(base + (int) ((104_729L * (++siguienteDni)) % 5_000_000));
        } while (!dnisUsados.add(dni));
        return dni;
    }

    private ReservaSemilla reserva(Salida s, Cliente titular) {
        return new ReservaSemilla(s, titular);
    }

    /** Arma una reserva coherente: importe, saldo, pagos, cupos, refugios, certificados y equipamiento. */
    private final class ReservaSemilla {
        private final Salida salida;
        private final Cliente titular;
        private final List<Persona> acompanantes = new ArrayList<>();
        private final Map<String, Integer> pedidos = new LinkedHashMap<>();
        private EstadoReserva estado = EstadoReserva.CONFIRMADA;
        private boolean enDosPagos;
        private boolean rechazoPrevio;
        private boolean refugio;
        private int diasAntes = 10;
        private EstadoCertificado[] certificados = {};

        ReservaSemilla(Salida salida, Cliente titular) {
            this.salida = salida;
            this.titular = titular;
        }

        ReservaSemilla con(Persona... personas) { acompanantes.addAll(List.of(personas)); return this; }
        ReservaSemilla total() { estado = EstadoReserva.CONFIRMADA; enDosPagos = false; return this; }
        ReservaSemilla enDosPagos() { estado = EstadoReserva.CONFIRMADA; enDosPagos = true; return this; }
        ReservaSemilla sena() { estado = EstadoReserva.SENADA; return this; }
        ReservaSemilla refugio() { refugio = true; return this; }
        ReservaSemilla rechazoPrevio() { rechazoPrevio = true; return this; }
        /** Días antes de la salida en que se hizo la reserva (si eso cae en el futuro, se usa "hace un rato"). */
        ReservaSemilla dias(int dias) { diasAntes = dias; return this; }
        ReservaSemilla equipo(String item, int cantidad) { pedidos.put(item, cantidad); return this; }
        /** Estado del certificado de cada participante, en orden (el titular primero). Los que faltan: Aprobado. */
        ReservaSemilla certificados(EstadoCertificado... estados) { certificados = estados; return this; }

        Reserva guardar() {
            Excursion e = salida.getExcursion();
            List<AsignacionRefugio> refugios = refugiosPorSalida.getOrDefault(salida.getIdSalida(), List.of());
            boolean conRefugio = refugio && !refugios.isEmpty();
            int n = 1 + acompanantes.size();

            BigDecimal porPersona = e.getPrecio();
            if (conRefugio) {
                for (AsignacionRefugio a : refugios) porPersona = porPersona.add(a.getRefugio().getPrecioNoche());
            }
            BigDecimal monto = porPersona.multiply(BigDecimal.valueOf(n));
            BigDecimal sena = monto.multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP);

            LocalDateTime fecha = salida.getFechaInicio().minusDays(diasAntes).atTime(9 + azar.nextInt(11), azar.nextInt(60));
            if (!fecha.isBefore(ahora)) fecha = ahora.minusHours(2 + azar.nextInt(20));

            Reserva r = new Reserva();
            r.setSalida(salida);
            r.setCliente(titular);
            r.setFechaHora(fecha);
            r.setEstadoReserva(estado);
            r.setCantidadParticipantes(n);
            r.setMontoTotal(monto);
            r.setSaldoPendiente(estado == EstadoReserva.SENADA ? monto.subtract(sena) : BigDecimal.ZERO);
            if (e.getDuracion() != null && e.getDuracion() > 1) {
                r.setModalidadAlojamiento(conRefugio ? ModalidadAlojamiento.REFUGIO : ModalidadAlojamiento.CARPA_PROPIA);
            }
            r = reservaRepository.save(r);

            // Pagos (CU-26)
            LocalDateTime pagado = fecha.plusMinutes(3);
            if (rechazoPrevio) {
                pago(r, estado == EstadoReserva.SENADA ? sena : monto,
                        estado == EstadoReserva.SENADA ? ModoPago.SENA : ModoPago.TOTAL, EstadoPago.RECHAZADO, fecha.plusMinutes(2));
                pagado = fecha.plusMinutes(5);
            }
            if (estado == EstadoReserva.SENADA) {
                pago(r, sena, ModoPago.SENA, EstadoPago.APROBADO, pagado);
            } else if (enDosPagos) {
                pago(r, sena, ModoPago.SENA, EstadoPago.APROBADO, pagado);
                LocalDateTime saldo = fecha.plusDays(Math.max(1, diasAntes / 2));
                LocalDateTime limite = salida.getFechaInicio().atTime(salida.getHoraInicio()).minusHours(73);
                if (saldo.isAfter(limite)) saldo = limite;
                if (saldo.isBefore(pagado)) saldo = pagado.plusHours(1);
                pago(r, monto.subtract(sena), ModoPago.SALDO, EstadoPago.APROBADO, saldo);
            } else {
                pago(r, monto, ModoPago.TOTAL, EstadoPago.APROBADO, pagado);
            }

            // Participantes (el titular siempre participa) y certificados (CU-24)
            List<Persona> personas = new ArrayList<>();
            personas.add(new Persona(titular.getDni(), titular.getNombre(), titular.getApellido(),
                    titular.getCorreoElectronico(), titular.getTelefono(), titular.getFechaNacimiento()));
            personas.addAll(acompanantes);
            int i = 0;
            for (Persona p : personas) {
                String correoParticipante = p.correo() != null ? p.correo() : titular.getCorreoElectronico();
                String telefono = p.correo() != null ? p.telefono() : titular.getTelefono();
                Participante part = participanteRepository.save(new Participante(r, p.dni(), p.nombre(), p.apellido(),
                        correoParticipante, telefono, p.nacimiento()));
                if (e.isRequiereCertificado()) {
                    EstadoCertificado ec = i < certificados.length ? certificados[i] : EstadoCertificado.APROBADO;
                    certificado(part, ec, fecha);
                }
                i++;
            }

            // Equipamiento prestado
            for (Map.Entry<String, Integer> pedido : pedidos.entrySet()) {
                Equipamiento item = equipo.get(pedido.getKey());
                if (item != null && pedido.getValue() > 0) {
                    asignacionEquipamientoRepository.save(new AsignacionEquipamiento(item, r,
                            Math.min(pedido.getValue(), n), EstadoAsignacion.CONSOLIDADO));
                }
            }

            // Cupos y plazas ocupadas
            salida.setCuposOcupados(salida.getCuposOcupados() + n);
            if (conRefugio) {
                for (AsignacionRefugio a : refugios) a.setPlazasOcupadas(a.getPlazasOcupadas() + n);
            }
            return r;
        }
    }

    private void pago(Reserva r, BigDecimal importe, ModoPago modo, EstadoPago estado, LocalDateTime fecha) {
        Pago p = new Pago(r, importe, modo);
        p.setEstadoPago(estado);
        p.setNroTransaccion((modo == ModoPago.REINTEGRO ? "REI-" : "SIM-")
                + Integer.toHexString(0x5A3C10 + 7919 * (++siguienteTransaccion)).toUpperCase(Locale.ROOT));
        p.setFechaPago(fecha);
        pagoRepository.save(p);
    }

    private void certificado(Participante p, EstadoCertificado estado, LocalDateTime reservada) {
        if (estado == EstadoCertificado.PENDIENTE) {
            CertificadoMedico c = new CertificadoMedico();
            c.setParticipante(p);
            c.setEstadoCertificado(EstadoCertificado.PENDIENTE);
            c.setFechaSubida(null);
            certificadoRepository.save(c);
            return;
        }
        LocalDateTime subida = reservada.plusDays(1 + azar.nextInt(3)).withHour(10 + azar.nextInt(10));
        if (subida.isAfter(ahora)) subida = ahora.minusHours(1 + azar.nextInt(5));
        String ruta = "certificados/certificado-" + p.getDni() + ".pdf";
        escribirCertificado(ruta, p, subida.toLocalDate());
        CertificadoMedico c = new CertificadoMedico(p, ruta, subida, subida.toLocalDate().plusYears(1), estado);
        if (estado == EstadoCertificado.RECHAZADO) {
            c.setMotivoRechazo("El certificado no tiene firma ni sello del médico. Cargá uno firmado.");
        }
        certificadoRepository.save(c);
    }

    /** Un PDF de una página para que el administrador pueda abrir el adjunto (CU-25). */
    private void escribirCertificado(String ruta, Participante p, LocalDate fecha) {
        try {
            Path destino = carpetaArchivos.resolve(ruta).normalize();
            Files.createDirectories(destino.getParent());
            String texto = "Certificado medico - " + sinAcentos(p.getApellido() + ", " + p.getNombre())
                    + " - DNI " + p.getDni() + " - Apto para actividad fisica de montana - "
                    + fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            Files.write(destino, pdf(texto));
        } catch (IOException ex) {
            log.warn("No se pudo crear el archivo de ejemplo {}: {}", ruta, ex.getMessage());
        }
    }

    private static String sinAcentos(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replaceAll("[()\\\\]", "");
    }

    private static byte[] pdf(String texto) {
        String contenido = "BT /F1 12 Tf 50 780 Td (" + texto + ") Tj ET";
        String[] objetos = {
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R "
                        + "/Resources << /Font << /F1 5 0 R >> >> >>",
                "<< /Length " + contenido.length() + " >>\nstream\n" + contenido + "\nendstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"};
        StringBuilder sb = new StringBuilder("%PDF-1.4\n");
        int[] offsets = new int[objetos.length];
        for (int i = 0; i < objetos.length; i++) {
            offsets[i] = sb.length();
            sb.append(i + 1).append(" 0 obj\n").append(objetos[i]).append("\nendobj\n");
        }
        int xref = sb.length();
        sb.append("xref\n0 ").append(objetos.length + 1).append("\n0000000000 65535 f \n");
        for (int o : offsets) sb.append(String.format("%010d 00000 n \n", o));
        sb.append("trailer\n<< /Size ").append(objetos.length + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    // =========================================================================
    //  Operación en terreno, valoraciones y estados finales
    // =========================================================================

    /** Salida finalizada: todos Presentes y Aptos con su equipamiento; el titular valora si estrellas > 0. */
    private void finalizada(Reserva r, int estrellas, String comentario) {
        acreditarTodos(r);
        if (estrellas > 0) {
            Participante titular = participantes(r).get(0);
            LocalDate fin = r.getSalida().getFechaFinalizacion();
            LocalDateTime cuando = fin.plusDays(1 + azar.nextInt(2)).atTime(9 + azar.nextInt(12), azar.nextInt(60));
            if (cuando.isAfter(ahora)) cuando = ahora.minusHours(1);
            valoracionRepository.save(new Valoracion(titular, estrellas, comentario, cuando));
        }
    }

    private List<Participante> participantes(Reserva r) {
        return participanteRepository.findByReservaIdReservaOrderByIdParticipanteAsc(r.getIdReserva());
    }

    /** CU-15: todos Presentes y Aptos, con su equipamiento obligatorio presentado (lo prestado, primero). */
    private void acreditarTodos(Reserva r) {
        List<Participante> lista = participantes(r);
        for (int i = 0; i < lista.size(); i++) acreditar(r, i, EstadoAsistencia.PRESENTE, EstadoRequisitos.APTO);
    }

    private void acreditar(Reserva r, int indice, EstadoAsistencia asistencia, EstadoRequisitos requisitos) {
        Participante p = participantes(r).get(indice);
        p.setEstadoAsistencia(asistencia);
        p.setEstadoRequisitos(asistencia == EstadoAsistencia.AUSENTE ? null : requisitos);
        participanteRepository.save(p);
        Set<Equipamiento> obligatorio = r.getSalida().getExcursion().getEquipamientoObligatorio();
        List<EntregaEquipamiento> previas = entregaEquipamientoRepository.findAll().stream()
                .filter(x -> x.getParticipante().getIdParticipante().equals(p.getIdParticipante())).toList();
        if (!previas.isEmpty()) {
            entregaEquipamientoRepository.deleteAll(previas);
            entregaEquipamientoRepository.flush(); // la misma clave se vuelve a insertar abajo
        }
        if (asistencia == EstadoAsistencia.AUSENTE) return;
        Map<Long, Integer> prestados = new HashMap<>();
        asignacionEquipamientoRepository.findByReservaIdReserva(r.getIdReserva())
                .forEach(a -> prestados.put(a.getEquipamiento().getIdEquipamiento(), a.getCantidadSolicitada()));
        boolean primero = true;
        for (Equipamiento item : obligatorio) {
            boolean loTiene = requisitos == EstadoRequisitos.APTO || !primero;
            primero = false;
            int prestado = loTiene && prestados.getOrDefault(item.getIdEquipamiento(), 0) > indice ? 1 : 0;
            entregaEquipamientoRepository.save(new EntregaEquipamiento(item, p, prestado, loTiene));
        }
    }

    /** CU-14 Cancelación: la salida queda Cancelada y sus reservas, Pendientes de devolución. */
    private void cancelarPorContingencia(Salida s, String tipo, String motivo, int diaRegistro) {
        String texto = tipo.equals("CLIMATICA") ? "climática" : "operativa";
        for (Reserva r : reservaRepository.findBySalidaIdSalidaAndEstadoReservaInOrderByIdReservaAsc(s.getIdSalida(), OCUPAN)) {
            r.setEstadoReserva(EstadoReserva.PENDIENTE_DEVOLUCION);
            r.setMotivoCancelacion("Cancelación de la salida por contingencia " + texto + ": " + motivo);
            asignacionEquipamientoRepository.findByReservaIdReserva(r.getIdReserva())
                    .forEach(a -> a.setEstadoAsignacion(EstadoAsignacion.LIBERADO));
        }
        s.setCuposOcupados(0);
        s.setEstadoSalida(EstadoSalida.CANCELADA);
        s.setMotivoCancelacion("Contingencia " + texto + ": " + motivo);
        liberarGuiasYRefugios(s);
        contingencia(s, tipo, "CANCELACION", motivo, diaRegistro, null, null);
    }

    /** CU-27: el administrador ya devolvió lo abonado. */
    private void devolver(Reserva r, int dia) {
        BigDecimal abonado = BigDecimal.ZERO;
        for (Pago p : pagoRepository.findByReservaIdReservaOrderByIdPagoAsc(r.getIdReserva())) {
            if (p.getEstadoPago() == EstadoPago.APROBADO) {
                abonado = abonado.add(p.getImporte());
                p.setEstadoPago(EstadoPago.DEVUELTO);
            }
        }
        pago(r, abonado, ModoPago.REINTEGRO, EstadoPago.APROBADO, hoy.plusDays(dia).atTime(11, 20));
        r.setEstadoReserva(EstadoReserva.DEVUELTA);
    }

    /** CU-23 / CU-30: la reserva se canceló (sin devolución) y se liberaron sus lugares. */
    private void cancelarPorCliente(Reserva r, String motivo, int dia) {
        Salida s = r.getSalida();
        s.setCuposOcupados(Math.max(0, s.getCuposOcupados() - r.getCantidadParticipantes()));
        if (r.getModalidadAlojamiento() == ModalidadAlojamiento.REFUGIO) {
            for (AsignacionRefugio a : refugiosPorSalida.getOrDefault(s.getIdSalida(), List.of())) {
                a.setPlazasOcupadas(Math.max(0, a.getPlazasOcupadas() - r.getCantidadParticipantes()));
            }
        }
        asignacionEquipamientoRepository.findByReservaIdReserva(r.getIdReserva())
                .forEach(a -> a.setEstadoAsignacion(EstadoAsignacion.LIBERADO));
        participantes(r).forEach(p -> {
            p.setEstadoAsistencia(null);
            p.setEstadoRequisitos(null);
        });
        r.setEstadoReserva(EstadoReserva.CANCELADA);
        r.setMotivoCancelacion(motivo);
    }

    /** CU-14 Reprogramación: la salida pasa a la fecha actual; antes era "diaAnterior". */
    private void reprogramar(Salida s, int diaAnterior, String tipo, String motivo, int diaRegistro,
                             List<Reserva> pendientes, List<Reserva> aceptaron) {
        pendientes.forEach(r -> r.setReprogramacionPendiente(true));
        aceptaron.forEach(r -> r.setReprogramacionPendiente(false));
        contingencia(s, tipo, "REPROGRAMACION", motivo, diaRegistro,
                hoy.plusDays(diaAnterior).atTime(s.getHoraInicio()), null);
    }

    /** CU-14 Modificación de recorrido. */
    private void modificarRecorrido(Salida s, String motivo, String recorrido, int diaRegistro) {
        s.setRecorrido(recorrido);
        contingencia(s, "OPERATIVA", "MODIFICACION", motivo, diaRegistro, null, recorrido);
    }

    private void contingencia(Salida s, String tipo, String resolucion, String motivo, int dia,
                              LocalDateTime fechaAnterior, String recorrido) {
        Contingencia c = new Contingencia(tipo, motivo, resolucion);
        c.setSalida(s);
        c.setFechaRegistro(hoy.plusDays(dia).atTime(9, 40));
        c.setFechaAnteriorSalida(fechaAnterior);
        c.setRecorridoAlternativo(recorrido);
        contingenciaRepository.save(c);
    }

    // =========================================================================
    //  Controles de coherencia (si algo no cierra, se informa y no se guarda nada)
    // =========================================================================

    private void verificar() {
        List<String> problemas = new ArrayList<>();
        Map<Long, List<LocalDateTime[]>> agenda = new HashMap<>();
        for (Salida s : salidas) {
            String id = "Salida #" + s.getIdSalida() + " (" + s.getExcursion().getNombre() + ")";
            if (s.getCupoBase() >= s.getCupoMaximo()) problemas.add(id + ": el cupo base no es menor al máximo.");
            if (s.getCupoTotal() > s.getCupoMaximo()) problemas.add(id + ": el cupo total supera al máximo.");
            if (s.getCuposOcupados() > s.getCupoTotal()) problemas.add(id + ": hay más ocupados que cupo total.");
            int ocupan = reservaRepository.findBySalidaIdSalidaAndEstadoReservaInOrderByIdReservaAsc(s.getIdSalida(), OCUPAN)
                    .stream().mapToInt(Reserva::getCantidadParticipantes).sum();
            if (s.getEstadoSalida() != EstadoSalida.CANCELADA && ocupan != s.getCuposOcupados()) {
                problemas.add(id + ": cupos ocupados " + s.getCuposOcupados() + " y reservas vigentes " + ocupan + ".");
            }
            for (AsignacionRefugio a : refugiosPorSalida.getOrDefault(s.getIdSalida(), List.of())) {
                if (a.getPlazasOcupadas() > a.getPlazasReservadas()) {
                    problemas.add(id + ": " + a.getRefugio().getNombre() + " tiene más plazas ocupadas que reservadas.");
                }
            }
            if (s.getEstadoSalida() == EstadoSalida.CANCELADA) continue;
            LocalDateTime desde = s.getFechaInicio().atTime(s.getHoraInicio());
            LocalDateTime hasta = s.getFechaFinalizacion().atTime(s.getHoraFinalizacion());
            for (Guia g : guiasPorSalida.getOrDefault(s.getIdSalida(), List.of())) {
                for (LocalDateTime[] otro : agenda.computeIfAbsent(g.getIdUsuario(), k -> new ArrayList<>())) {
                    if (desde.isBefore(otro[1]) && otro[0].isBefore(hasta)) {
                        problemas.add(id + ": " + g.getNombre() + " " + g.getApellido() + " tiene otra salida superpuesta.");
                    }
                }
                agenda.get(g.getIdUsuario()).add(new LocalDateTime[]{desde, hasta});
            }
        }
        if (!problemas.isEmpty()) {
            throw new IllegalStateException("Los datos de la exposición no son coherentes:\n - "
                    + String.join("\n - ", problemas));
        }
    }
}
