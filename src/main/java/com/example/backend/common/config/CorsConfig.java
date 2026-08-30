package com.example.backend.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    // Antes hardcodeado en Java — actualizar el origen real de producción
    // requería recompilar. Ahora es ${CORS_ALLOWED_ORIGINS:...} (ver
    // application-dev.properties / application-prod.properties), igual que
    // ya se hace con websocket.allowed-origins.
    @Value("${cors.allowed-origins}")
    private String allowedOriginsCsv;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Orígenes permitidos (frontend dev + cualquier otro que uses).
        // Nota: esto solo aplica a requests que llegan con header Origin
        // (navegadores). Los clientes HTTP nativos de la app móvil (Android/iOS)
        // no envían Origin, así que no les afecta el CORS.
        List<String> allowedOrigins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOrigins(allowedOrigins);

        // Métodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Cabeceras permitidas en el request
        config.setAllowedHeaders(List.of("*"));

        // Permite enviar cookies / Authorization header
        config.setAllowCredentials(true);

        // Cuánto tiempo el browser cachea la respuesta preflight (en segundos)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);   // aplica a todas las rutas
        return source;
    }
}