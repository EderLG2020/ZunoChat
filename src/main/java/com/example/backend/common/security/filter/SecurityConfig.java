package com.example.backend.common.security.filter;

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

/**
 * Configuración de seguridad.
 *
 * @EnableMethodSecurity habilita:
 *   @PreAuthorize("hasRole('ADMIN')")
 *   @PreAuthorize("hasAuthority('dashboard:editar')")
 *   @PreAuthorize("hasAnyAuthority('usuarios:ver', 'usuarios:bannear')")
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // ← necesario para @PreAuthorize en controllers
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth

                        // 🔓 Rutas públicas
                        .requestMatchers(
                            "/auth/register",
                            "/auth/verify-otp",
                            "/auth/login"
                        ).permitAll()

                        // El resto requiere autenticación.
                        // El control fino se delega a @PreAuthorize en cada controller.
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Bean de BCrypt para inyectar en servicios.
     * Factor de coste 12 (buen balance seguridad/rendimiento en 2024).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
