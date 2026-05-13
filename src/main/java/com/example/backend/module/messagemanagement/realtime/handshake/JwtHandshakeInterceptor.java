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
 * Extrae el token del query param ?token=<jwt> y lo almacena
 * en los atributos de la sesión WS para uso posterior.
 *
 * La autenticación real (Principal) se establece en StompAuthChannelInterceptor.
 * Este interceptor solo pre-valida y guarda userId/username en los atributos.
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
        if (query == null) return true; // el token también puede venir en el frame CONNECT

        String token = extractTokenFromQuery(query);
        if (token == null) return true;

        try {
            Claims claims = jwtService.extractClaims(token);
            attributes.put("username", claims.getSubject());
            Number userId = (Number) claims.get("userId");
            if (userId != null) attributes.put("userId", userId.longValue());
            log.debug("Handshake WS pre-validado: user={}", claims.getSubject());
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