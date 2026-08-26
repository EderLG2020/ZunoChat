package com.example.backend.common.security.filter;

import com.example.backend.common.config.CorsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private CorsConfig corsConfig;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth

                        // Rutas públicas REST
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/verify-otp",
                                "/api/auth/login",
                                "/api/auth/resend-otp",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/refresh"
                        ).permitAll()

                        // WebSocket — la autenticación se hace vía STOMP (StompAuthChannelInterceptor)
                        .requestMatchers(
                                "/ws/**",
                                "/ws/info/**"
                        ).permitAll()

                        // Archivos servidos por el proveedor "local" (StaticUploadsConfig) — deben
                        // ser públicos para que un <img src="..."> los cargue sin Authorization.
                        // POST /api/uploads (el endpoint que los sube) NO está acá — sigue
                        // cayendo en anyRequest().authenticated() de abajo.
                        .requestMatchers("/uploads/**").permitAll()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}