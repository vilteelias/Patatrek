package com.patatrek.backend.config;

import com.patatrek.backend.usuario.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuentas de ejemplo para el módulo de Autenticación:
 *  - Crea el administrador admin@patatrek.com si no hay ninguno.
 *  - A los guías y clientes de ejemplo que todavía no tienen contraseña ("SIN_DEFINIR"),
 *    les asigna la contraseña de demostración (patatrek.contrasenia-demo), guardada con BCrypt.
 * Se ejecuta en cada arranque, pero solo toca las cuentas sin contraseña.
 */
@Component
@Order(10)
@ConditionalOnProperty(name = "patatrek.cargar-datos-demo", havingValue = "true", matchIfMissing = true)
public class DatosInicialesUsuarios implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosInicialesUsuarios.class);

    private final AdministradorRepository administradorRepository;
    private final GuiaRepository guiaRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final String contraseniaDemo;

    public DatosInicialesUsuarios(AdministradorRepository administradorRepository, GuiaRepository guiaRepository,
                                  ClienteRepository clienteRepository, PasswordEncoder passwordEncoder,
                                  @Value("${patatrek.contrasenia-demo:Patatrek2026}") String contraseniaDemo) {
        this.administradorRepository = administradorRepository;
        this.guiaRepository = guiaRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.contraseniaDemo = contraseniaDemo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String hash = passwordEncoder.encode(contraseniaDemo);
        if (administradorRepository.count() == 0) {
            administradorRepository.save(new Administrador("25111333", "Valeria", "Suárez",
                    "admin@patatrek.com", hash, "2804-500100"));
        }
        int actualizados = 0;
        for (Guia g : guiaRepository.findAll()) {
            if (sinContrasenia(g.getContrasenia())) {
                g.setContrasenia(hash);
                actualizados++;
            }
        }
        for (Cliente c : clienteRepository.findAll()) {
            if (sinContrasenia(c.getContrasenia())) {
                c.setContrasenia(hash);
                actualizados++;
            }
        }
        if (actualizados > 0) {
            log.info("Se asignó la contraseña de demostración a {} cuentas de ejemplo.", actualizados);
        }
    }

    private static boolean sinContrasenia(String hash) {
        return hash == null || !hash.startsWith("$2");
    }
}
