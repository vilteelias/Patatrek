package com.patatrek.backend.auth;

import com.patatrek.backend.auth.dto.RegistroRequest;
import com.patatrek.backend.excepcion.CredencialesInvalidasException;
import com.patatrek.backend.excepcion.DatosInvalidosException;
import com.patatrek.backend.excepcion.ReglaNegocioException;
import com.patatrek.backend.usuario.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Paquete "Autenticación": CU-01 Registrar cliente y CU-02 Iniciar sesión.
 * (CU-03 Cerrar sesión lo resuelve el controlador invalidando la sesión.)
 * Las contraseñas se guardan con hash BCrypt, nunca en texto plano.
 */
@Service
public class AuthService {

    static final String CREDENCIALES_INCORRECTAS = "El correo electrónico o la contraseña son incorrectos.";

    private static final Pattern PATRON_DNI = Pattern.compile("\\d{7,8}");
    private static final Pattern PATRON_CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PATRON_TELEFONO = Pattern.compile("^[0-9+()\\-\\s]{6,20}$");

    private final AdministradorRepository administradorRepository;
    private final GuiaRepository guiaRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AdministradorRepository administradorRepository, GuiaRepository guiaRepository,
                       ClienteRepository clienteRepository, PasswordEncoder passwordEncoder) {
        this.administradorRepository = administradorRepository;
        this.guiaRepository = guiaRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =====================================================================
    //  CU-02 Iniciar sesión
    // =====================================================================

    @Transactional(readOnly = true)
    public UsuarioSesion autenticar(String correo, String contrasenia) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (vacio(correo)) errores.put("correoElectronico", "Ingresá tu correo electrónico.");
        if (vacio(contrasenia)) errores.put("contrasenia", "Ingresá tu contraseña.");
        if (!errores.isEmpty()) {
            throw new DatosInvalidosException("Completá los datos marcados.", errores);
        }
        String c = correo.trim();

        Optional<Administrador> admin = administradorRepository.findByCorreoElectronicoIgnoreCase(c);
        if (admin.isPresent() && coincide(contrasenia, admin.get().getContrasenia())) {
            Administrador a = admin.get();
            return new UsuarioSesion(a.getIdUsuario(), Rol.ADMINISTRADOR, a.getNombre(), a.getApellido(),
                    a.getCorreoElectronico(), a.getDni());
        }
        Optional<Guia> guia = guiaRepository.findByCorreoElectronicoIgnoreCase(c);
        if (guia.isPresent() && coincide(contrasenia, guia.get().getContrasenia())) {
            Guia g = guia.get();
            return new UsuarioSesion(g.getIdUsuario(), Rol.GUIA, g.getNombre(), g.getApellido(),
                    g.getCorreoElectronico(), g.getDni());
        }
        Optional<Cliente> cliente = clienteRepository.findByCorreoElectronicoIgnoreCase(c);
        if (cliente.isPresent() && coincide(contrasenia, cliente.get().getContrasenia())) {
            return sesionDe(cliente.get());
        }
        // RF-02: si los datos son incorrectos se muestra un mensaje de error (sin decir cuál de los dos falló)
        throw new CredencialesInvalidasException(CREDENCIALES_INCORRECTAS);
    }

    private boolean coincide(String contrasenia, String hash) {
        // Las cuentas sin contraseña definida (hash que no es BCrypt) no pueden ingresar
        return hash != null && hash.startsWith("$2") && passwordEncoder.matches(contrasenia, hash);
    }

    // =====================================================================
    //  CU-01 Registrar cliente
    // =====================================================================

    @Transactional
    public UsuarioSesion registrar(RegistroRequest d) {
        if (d == null) {
            throw new DatosInvalidosException("Completá los datos marcados.", Map.of("nombre", "Ingresá tu nombre."));
        }
        Map<String, String> errores = new LinkedHashMap<>();
        if (vacio(d.nombre())) errores.put("nombre", "Ingresá tu nombre.");
        else if (d.nombre().trim().length() > 60) errores.put("nombre", "El nombre puede tener hasta 60 caracteres.");
        if (vacio(d.apellido())) errores.put("apellido", "Ingresá tu apellido.");
        else if (d.apellido().trim().length() > 60) errores.put("apellido", "El apellido puede tener hasta 60 caracteres.");

        if (vacio(d.dni())) errores.put("dni", "Ingresá tu DNI.");
        else if (!PATRON_DNI.matcher(d.dni().trim()).matches()) errores.put("dni", "El DNI debe tener 7 u 8 números, sin puntos.");

        if (vacio(d.correoElectronico())) errores.put("correoElectronico", "Ingresá tu correo electrónico.");
        else if (!PATRON_CORREO.matcher(d.correoElectronico().trim()).matches())
            errores.put("correoElectronico", "Ingresá un correo válido, por ejemplo nombre@mail.com.");

        if (vacio(d.telefono())) errores.put("telefono", "Ingresá tu teléfono.");
        else if (!PATRON_TELEFONO.matcher(d.telefono().trim()).matches())
            errores.put("telefono", "Ingresá solo números, espacios o guiones.");

        if (d.fechaNacimiento() == null) errores.put("fechaNacimiento", "Ingresá tu fecha de nacimiento.");
        else if (!d.fechaNacimiento().isBefore(LocalDate.now()))
            errores.put("fechaNacimiento", "La fecha de nacimiento debe ser anterior a hoy.");

        if (vacio(d.contrasenia())) errores.put("contrasenia", "Ingresá una contraseña.");
        else if (d.contrasenia().length() < 8 || !d.contrasenia().matches(".*[A-Za-z].*") || !d.contrasenia().matches(".*\\d.*"))
            errores.put("contrasenia", "La contraseña debe tener al menos 8 caracteres, con letras y números.");
        if (vacio(d.confirmarContrasenia())) errores.put("confirmarContrasenia", "Repetí la contraseña.");
        else if (d.contrasenia() != null && !d.contrasenia().equals(d.confirmarContrasenia()))
            errores.put("confirmarContrasenia", "Las contraseñas no coinciden.");

        if (!errores.isEmpty()) {
            throw new DatosInvalidosException("Revisá los datos marcados.", errores);
        }

        // RF-01: DNI y correo no pueden estar registrados previamente
        String dni = d.dni().trim();
        String correo = d.correoElectronico().trim().toLowerCase(Locale.ROOT);
        Map<String, String> duplicados = new LinkedHashMap<>();
        if (clienteRepository.existsByDni(dni)) {
            duplicados.put("dni", "Ya existe una cuenta registrada con este DNI.");
        }
        if (clienteRepository.existsByCorreoElectronicoIgnoreCase(correo)
                || guiaRepository.findByCorreoElectronicoIgnoreCase(correo).isPresent()
                || administradorRepository.existsByCorreoElectronicoIgnoreCase(correo)) {
            duplicados.put("correoElectronico", "Ya existe una cuenta registrada con este correo electrónico.");
        }
        if (!duplicados.isEmpty()) {
            throw new ReglaNegocioException("No se pudo crear la cuenta: " + String.join(" ", duplicados.values()),
                    duplicados);
        }

        Cliente c = new Cliente(dni, d.nombre().trim(), d.apellido().trim(), correo, d.telefono().trim(),
                d.fechaNacimiento());
        c.setContrasenia(passwordEncoder.encode(d.contrasenia()));
        return sesionDe(clienteRepository.save(c));
    }

    private static UsuarioSesion sesionDe(Cliente c) {
        return new UsuarioSesion(c.getIdUsuario(), Rol.CLIENTE, c.getNombre(), c.getApellido(),
                c.getCorreoElectronico(), c.getDni());
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
