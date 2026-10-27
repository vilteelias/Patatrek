package com.patatrek.backend.config;

import com.patatrek.backend.excursion.Excursion;
import com.patatrek.backend.excursion.ExcursionRepository;
import com.patatrek.backend.salida.EstadoSalida;
import com.patatrek.backend.salida.Salida;
import com.patatrek.backend.salida.SalidaRepository;
import com.patatrek.backend.usuario.Cliente;
import com.patatrek.backend.usuario.ClienteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Datos de ejemplo para mostrar la Reserva y el Pago (CU-18, CU-26):
 * la clienta de demostración Sofía Ibarra y algunas salidas futuras que cubren los casos del CU-18:
 *  - Sendero Laguna del Bosque en 7 días: 1 día, admite menores, sin equipamiento (caso simple).
 *  - Ascenso Cerro Perito Moreno en 9 días: quedan 2 cupos, no admite menores, pide Casco y Guantes.
 *  - Ascenso Cerro Perito Moreno en 2 días: no se puede reservar (72 horas o menos).
 * Además ya existen (DatosInicialesGuia) la Travesía Cerro Alto en 20 días con refugio y el Circuito Lagos en 10 días.
 * Se carga una sola vez: si la clienta de demostración ya existe, no hace nada.
 */
@Component
@Order(3)
@ConditionalOnProperty(name = "patatrek.cargar-datos-demo", havingValue = "true", matchIfMissing = true)
public class DatosInicialesReserva implements CommandLineRunner {

    private final ClienteRepository clienteRepository;
    private final ExcursionRepository excursionRepository;
    private final SalidaRepository salidaRepository;
    private final String correoClienteDemo;

    public DatosInicialesReserva(ClienteRepository clienteRepository, ExcursionRepository excursionRepository,
                                 SalidaRepository salidaRepository,
                                 @Value("${patatrek.cliente-demo-correo:sofia.ibarra@mail.com}") String correoClienteDemo) {
        this.clienteRepository = clienteRepository;
        this.excursionRepository = excursionRepository;
        this.salidaRepository = salidaRepository;
        this.correoClienteDemo = correoClienteDemo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (clienteRepository.findByCorreoElectronicoIgnoreCase(correoClienteDemo).isPresent()) {
            return;
        }
        clienteRepository.save(new Cliente("38555777", "Sofía", "Ibarra", correoClienteDemo, "2804-667788",
                LocalDate.of(1994, 3, 22)));

        LocalDate hoy = LocalDate.now();
        Map<String, Excursion> excursiones = excursionRepository.findAllByOrderByNombreAsc().stream()
                .collect(Collectors.toMap(Excursion::getNombre, Function.identity()));

        Excursion laguna = excursiones.get("Sendero Laguna del Bosque");
        if (laguna != null) {
            salida(laguna, hoy.plusDays(7), LocalTime.of(9, 0), LocalTime.of(14, 0), 12, 3);
        }
        Excursion perito = excursiones.get("Ascenso Cerro Perito Moreno");
        if (perito != null) {
            salida(perito, hoy.plusDays(9), LocalTime.of(8, 0), LocalTime.of(17, 0), 14, 12);
            salida(perito, hoy.plusDays(2), LocalTime.of(8, 0), LocalTime.of(17, 0), 14, 5);
        }
    }

    private void salida(Excursion excursion, LocalDate dia, LocalTime desde, LocalTime hasta, int cupoTotal, int ocupados) {
        Salida s = new Salida();
        s.setExcursion(excursion);
        s.setFechaInicio(dia);
        s.setFechaFinalizacion(dia);
        s.setHoraInicio(desde);
        s.setHoraFinalizacion(hasta);
        s.setCupoBase(Math.min(8, cupoTotal));
        s.setCupoMaximo(cupoTotal);
        s.setCupoTotal(cupoTotal);
        s.setCuposOcupados(ocupados);
        s.setEstadoSalida(EstadoSalida.DISPONIBLE);
        s.setRecorrido(excursion.getRecorridoBase());
        salidaRepository.save(s);
    }
}
