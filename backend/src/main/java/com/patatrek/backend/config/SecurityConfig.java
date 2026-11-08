package com.patatrek.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Seguridad (módulo de Autenticación: CU-01, CU-02, CU-03).
 *
 * - El inicio de sesión guarda el usuario y su rol en la sesión HTTP.
 * - Las contraseñas se guardan con BCrypt.
 * - patatrek.seguridad.exigir-login=false (por ahora): las rutas NO se bloquean, así las pantallas
 *   que todavía no usan login siguen funcionando. Cuando todo el equipo esté integrado, se pone en true y
 *   cada rol solo puede usar sus rutas (/api/admin/** → Administrador, /api/guia/** → Guía, etc.).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filtroDeSeguridad(HttpSecurity http, SecurityContextRepository repositorio,
                                                 @Value("${patatrek.seguridad.exigir-login:false}") boolean exigirLogin)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)   // API REST consumida por el frontend propio (mismo origen vía Vite)
                .cors(Customizer.withDefaults())         // usa la configuración CORS de WebConfig
                .securityContext(c -> c.securityContextRepository(repositorio))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable) // el cierre de sesión lo hace /api/auth/logout
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        if (exigirLogin) {
            http.authorizeHttpRequests(rutas -> rutas
                    .requestMatchers("/api/auth/**", "/uploads/**").permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/excursiones/**").permitAll()
                    .requestMatchers("/api/valoraciones/**").permitAll()          // participante sin cuenta (enlace)
                    .requestMatchers("/api/participante/**").permitAll()          // participante: ID de reserva + DNI
                    .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                    .requestMatchers("/api/guia/**").hasRole("GUIA")
                    .requestMatchers("/api/cliente/**", "/api/reservas/**").hasRole("CLIENTE")
                    .anyRequest().authenticated());
        } else {
            http.authorizeHttpRequests(rutas -> rutas.anyRequest().permitAll());
        }
        return http.build();
    }
}
