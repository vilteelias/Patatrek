package com.patatrek.backend.config;

import com.patatrek.backend.equipamiento.Equipamiento;
import com.patatrek.backend.equipamiento.EquipamientoRepository;
import com.patatrek.backend.excursion.Dificultad;
import com.patatrek.backend.excursion.EstadoExcursion;
import com.patatrek.backend.excursion.Excursion;
import com.patatrek.backend.excursion.ExcursionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Carga el catálogo de equipamiento precargado (precondición del CU-05) y
 * las excursiones del prototipo, solo si las tablas están vacías.
 * Se desactiva con patatrek.cargar-datos-demo=false
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "patatrek.cargar-datos-demo", havingValue = "true", matchIfMissing = true)
public class DatosIniciales implements CommandLineRunner {

    private final EquipamientoRepository equipamientoRepository;
    private final ExcursionRepository excursionRepository;

    public DatosIniciales(EquipamientoRepository equipamientoRepository, ExcursionRepository excursionRepository) {
        this.equipamientoRepository = equipamientoRepository;
        this.excursionRepository = excursionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (equipamientoRepository.count() == 0) {
            equipamientoRepository.saveAll(List.of(
                    new Equipamiento("Bastones de trekking", "Par de bastones regulables", 30),
                    new Equipamiento("Campera cortavientos", "Campera impermeable rompeviento", 20),
                    new Equipamiento("Casco", "Casco de montaña certificado", 25),
                    new Equipamiento("Guantes térmicos", "Par de guantes para bajas temperaturas", 30),
                    new Equipamiento("Linterna frontal", "Linterna LED de cabeza", 30),
                    new Equipamiento("Botella de agua reutilizable", "Botella de 1 litro", 40),
                    new Equipamiento("Protector solar", "Protector FPS 50", 40),
                    new Equipamiento("Repelente de insectos", "Repelente en aerosol", 40),
                    new Equipamiento("Kit de primeros auxilios personal", "Botiquín individual", 20),
                    new Equipamiento("Bolsa de dormir", "Bolsa de dormir de tres estaciones", 20),
                    new Equipamiento("Colchoneta aislante", "Colchoneta térmica enrollable", 20),
                    new Equipamiento("Mochila 40 L", "Mochila de trekking de 40 litros", 15)
            ));
        }

        if (excursionRepository.count() == 0) {
            Map<String, Equipamiento> eq = equipamientoRepository.findAll().stream()
                    .collect(Collectors.toMap(Equipamiento::getNombre, Function.identity()));

            excursionRepository.saveAll(List.of(
                    excursion("Travesía Cerro Alto — Río Senguer",
                            "Tres días de trekking entre valles y refugios de montaña, con vistas al Río Senguer.",
                            "Terreno mixto de ripio y sendero de montaña. Desnivel acumulado de 1.200 m. Exigencia física media.",
                            "Base Alto Río Senguer – Refugio Valle Escondido – Mirador Cerro Alto – regreso por el río.",
                            Dificultad.MEDIA, 3, "185000", true, false,
                            items(eq, "Bastones de trekking", "Bolsa de dormir", "Campera cortavientos")),
                    excursion("Circuito Lagos Patagónicos",
                            "Una travesía de cinco días conectando lagos glaciares y bosques nativos de la región.",
                            "Jornadas de 6 a 8 horas de caminata con mochila. Cruces de arroyos y tramos de pendiente pronunciada.",
                            "Lago Fontana – Lago La Plata – Paso del Bosque – Refugio Los Coihues – regreso.",
                            Dificultad.ALTA, 5, "310000", true, false,
                            items(eq, "Bastones de trekking", "Bolsa de dormir",
                                    "Colchoneta aislante", "Linterna frontal")),
                    excursion("Ascenso Cerro Perito Moreno",
                            "Una jornada exigente de ascenso con vistas panorámicas de la cordillera.",
                            "Ascenso de 900 m de desnivel en terreno rocoso. Se requiere buena condición física.",
                            "Base del cerro – Filo norte – Cumbre – descenso por la misma ruta.",
                            Dificultad.ALTA, 1, "95000", true, false,
                            items(eq, "Casco", "Guantes térmicos")),
                    excursion("Sendero Laguna del Bosque",
                            "Caminata de baja exigencia, ideal para familias, entre bosque nativo y laguna.",
                            "Sendero señalizado de 8 km con pendiente suave. Apto para principiantes.",
                            "Portada del parque – Bosque de lengas – Laguna del Bosque – regreso circular.",
                            Dificultad.BAJA, 1, "42000", false, true, List.of()),
                    inactiva(excursion("Travesía Río Azul",
                            "Dos días siguiendo el cauce del Río Azul entre cañadones y estepa.",
                            "Terreno de estepa con cruces de río. Exigencia física media.",
                            "Puesto Río Azul – Cañadón Grande – Campamento Las Bardas – regreso.",
                            Dificultad.MEDIA, 2, "150000", true, false, List.of()),
                            "Discontinuada por cierre del acceso al campamento.")
            ));
        }
    }

    private Excursion excursion(String nombre, String general, String tecnica, String recorrido,
                                Dificultad dificultad, int dias, String precio,
                                boolean certificado, boolean menores, List<Equipamiento> equipo) {
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
        e.setRequiereEquipamiento(!equipo.isEmpty());
        e.setEquipamientoObligatorio(new LinkedHashSet<>(equipo));
        return e;
    }

    /** Busca los ítems por nombre; si alguno no existe en el catálogo, lo ignora. */
    private List<Equipamiento> items(Map<String, Equipamiento> catalogo, String... nombres) {
        return java.util.Arrays.stream(nombres).map(catalogo::get).filter(java.util.Objects::nonNull).toList();
    }

    private Excursion inactiva(Excursion e, String motivo) {
        e.setEstadoExcursion(EstadoExcursion.INACTIVA);
        e.setMotivoBaja(motivo);
        return e;
    }
}
