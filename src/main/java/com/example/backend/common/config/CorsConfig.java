package com.example.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Orígenes permitidos (frontend dev + cualquier otro que uses).
        // Nota: esto solo aplica a requests que llegan con header Origin
        // (navegadores). Los clientes HTTP nativos de la app móvil (Android/iOS)
        // no envían Origin, así que no les afecta el CORS — se agregan aquí los
        // puertos de `expo start --web` para poder probar esa app en el navegador.
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost:4200",
                "http://localhost:8081",
                "http://localhost:19006"
        ));

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