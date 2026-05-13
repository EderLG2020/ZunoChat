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

/**
 * Interceptor STOMP: valida JWT en el frame CONNECT.
 * Autentica el principal del WebSocket para que @MessageMapping
 * pueda acceder al usuario autenticado.
 *
 * Flujo:
 *   CONNECT frame → header Authorization: Bearer <token>
 *       → valida JWT → establece Principal en la sesión WS
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
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    Claims claims = jwtService.extractClaims(token);
                    String username = claims.getSubject();
                    String role = claims.get("role", String.class);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            );
                    // Almacena el userId como detalle para recuperarlo luego
                    Number userId = (Number) claims.get("userId");
                    auth.setDetails(userId != null ? userId.longValue() : null);

                    accessor.setUser(auth);
                    log.debug("WS CONNECT autenticado: user={}", username);

                } catch (Exception e) {
                    log.warn("WS CONNECT rechazado — token inválido: {}", e.getMessage());
                    throw new IllegalArgumentException("Token WS inválido");
                }
            }
        }
        return message;
    }
}