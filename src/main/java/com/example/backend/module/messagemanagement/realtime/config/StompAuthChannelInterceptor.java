package com.example.backend.module.messagemanagement.realtime.config;

import com.example.backend.common.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Interceptor STOMP: autenticad el Principal en el frame CONNECT.
 *
 * Estrategia de autenticación (en orden de prioridad):
 *
 *   1. Header "Authorization: Bearer <token>" en el frame CONNECT
 *      → forma estándar, preferida para clientes que controlan los headers STOMP.
 *
 *   2. Atributos de sesión WS (fallback)
 *      → el JwtHandshakeInterceptor ya validó el ?token= en el HTTP handshake
 *        y guardó username/userId/roles en los atributos de la sesión.
 *        Si el cliente conecta por query param peros no envía el header Authorization,
 *        se usa esta información para construir el Principal igualmente.
 *
 * Si ninguna fuente provee credenciales, el frame CONNECT se rechaza.
 */
@Slf4j
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // ── Fuente 1: header Authorization del frame CONNECT ─────────────
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    UsernamePasswordAuthenticationToken auth = buildAuthFromToken(token);
                    accessor.setUser(auth);
                    log.debug("WS CONNECT autenticado vía header: user={}", auth.getName());
                    return message;
                } catch (Exception e) {
                    log.warn("WS CONNECT rechazado — token en header inválido: {}", e.getMessage());
                    throw new IllegalArgumentException("Token WS inválido");
                }
            }

            // ── Fuente 2: atributos de sesión (query param ?token= ya validado) ─
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                String username = (String) sessionAttributes.get("username");
                Long   userId   = (Long)   sessionAttributes.get("userId");
                String role     = (String) sessionAttributes.get("role");

                if (username != null && userId != null) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    List.of(new SimpleGrantedAuthority(
                                            "ROLE_" + (role != null ? role : "USER")))
                            );
                    auth.setDetails(userId);
                    accessor.setUser(auth);
                    log.debug("WS CONNECT autenticado vía session attrs (query param): user={}", username);
                    return message;
                }
            }

            // ── Sin credenciales ─────────────────────────────────────────────
            log.warn("WS CONNECT rechazado — sin token en header ni en query param");
            throw new IllegalArgumentException("Token WS requerido");
        }

        return message;
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private UsernamePasswordAuthenticationToken buildAuthFromToken(String token) {
        Claims claims   = jwtService.extractClaims(token);
        String username = claims.getSubject();
        String role     = claims.get("role", String.class);
        Number userId   = (Number) claims.get("userId");

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER")))
                );
        auth.setDetails(userId != null ? userId.longValue() : null);
        return auth;
    }
}