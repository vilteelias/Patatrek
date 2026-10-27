package com.patatrek.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración TEMPORAL de Spring Security.
 *
 * Al tener spring-boot-starter-security en el pom, Spring bloquea todas las rutas por defecto
 * (pide usuario y contraseña). Mientras no esté hecho el módulo de Autenticación (CU-01, CU-02, CU-03),
 * se permite el acceso a todas las rutas para poder usar y mostrar el sistema.
 *
 * En el módulo de Autenticación se reemplaza por: login, roles (Administrador, Cliente, Guía)
 * y protección de /api/admin/** solo para el rol Administrador.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filtroDeSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)   // la API REST no usa formularios de sesión
                .cors(Customizer.withDefaults())         // usa la configuración CORS de WebConfig
                .authorizeHttpRequests(rutas -> rutas.anyRequest().permitAll())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
