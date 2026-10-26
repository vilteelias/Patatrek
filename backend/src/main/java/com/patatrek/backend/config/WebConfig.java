package com.patatrek.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * - CORS: permite que el frontend de Vite consuma la API.
 * - /uploads/**: sirve las imágenes promocionales guardadas en disco.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${patatrek.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${patatrek.carpeta-uploads:uploads}")
    private String carpetaUploads;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(frontendUrl)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String ubicacion = Path.of(carpetaUploads).toAbsolutePath().normalize().toUri().toString();
        if (!ubicacion.endsWith("/")) {
            ubicacion += "/"; // Spring necesita la barra final para tratarla como carpeta
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(ubicacion);
    }
}
