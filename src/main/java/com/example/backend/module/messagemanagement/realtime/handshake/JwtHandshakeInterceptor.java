package com.example.backend.module.messagemanagement.realtime.handshake;

import com.example.backend.common.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Interceptor HTTP del handshake WebSocket.
 *
 * Extrae el token del querys param ?token=<jwt> y guarda
 * username, userId y role en los atributos de la sesión WS.
 *
 * Estos atributos son usados por StompAuthChannelInterceptor como
 * fallback cuando el cliente no envía el header Authorization en el
 * frame CONNECT (caso típico de clientes que pasan el token solo por URL).
 */
@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtService jwtService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        String query = request.getURI().getQuery();
        if (query == null) return true;

        String token = extractTokenFromQuery(query);
        if (token == null) return true;

        try {
            Claims claims = jwtService.extractClaims(token);

            String username = claims.getSubject();
            Number userId   = (Number) claims.get("userId");
            String role     = claims.get("role", String.class);

            attributes.put("username", username);
            if (userId != null) attributes.put("userId", userId.longValue());
            if (role   != null) attributes.put("role",   role);

            log.debug("Handshake WS validado: user={} role={}", username, role);
        } catch (Exception e) {
            log.warn("Handshake WS — token inválido en query param: {}", e.getMessage());
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}

    private String extractTokenFromQuery(String query) {
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                return param.substring(6);
            }
        }
        return null;
    }
}