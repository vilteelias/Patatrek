package com.patatrek.backend.controller;

import com.patatrek.backend.auth.AuthService;
import com.patatrek.backend.auth.UsuarioActual;
import com.patatrek.backend.auth.UsuarioSesion;
import com.patatrek.backend.auth.dto.LoginRequest;
import com.patatrek.backend.auth.dto.RegistroRequest;
import com.patatrek.backend.auth.dto.SesionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CU-01 Registrar cliente, CU-02 Iniciar sesión y CU-03 Cerrar sesión.
 * La sesión se guarda del lado del servidor (cookie JSESSIONID); el frontend no guarda contraseñas ni tokens.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioActual usuarioActual;
    private final SecurityContextRepository securityContextRepository;
    private final boolean exigirLogin;

    public AuthController(AuthService authService, UsuarioActual usuarioActual,
                          SecurityContextRepository securityContextRepository,
                          @Value("${patatrek.seguridad.exigir-login:false}") boolean exigirLogin) {
        this.authService = authService;
        this.usuarioActual = usuarioActual;
        this.securityContextRepository = securityContextRepository;
        this.exigirLogin = exigirLogin;
    }

    @GetMapping("/sesion")
    public SesionResponse sesion() {
        return new SesionResponse(usuarioActual.obtener().orElse(null), exigirLogin);
    }

    @PostMapping("/login")
    public SesionResponse login(@RequestBody(required = false) LoginRequest datos,
                                HttpServletRequest request, HttpServletResponse response) {
        UsuarioSesion u = authService.autenticar(datos == null ? null : datos.correoElectronico(),
                datos == null ? null : datos.contrasenia());
        iniciarSesion(u, request, response);
        return new SesionResponse(u, exigirLogin);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionResponse registro(@RequestBody(required = false) RegistroRequest datos,
                                   HttpServletRequest request, HttpServletResponse response) {
        UsuarioSesion u = authService.registrar(datos);
        iniciarSesion(u, request, response); // queda con la sesión iniciada al registrarse
        return new SesionResponse(u, exigirLogin);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
    }

    private void iniciarSesion(UsuarioSesion u, HttpServletRequest request, HttpServletResponse response) {
        // Nueva sesión al iniciar sesión (evita "session fixation")
        HttpSession anterior = request.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        request.getSession(true);

        var autenticacion = new UsernamePasswordAuthenticationToken(u, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.rol().name())));
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);
        SecurityContextHolder.setContext(contexto);
        securityContextRepository.saveContext(contexto, request, response);
    }
}
