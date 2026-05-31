package com.example.backend.common.util;

import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.AppCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Utilidad estática para extraer datos del JWT en cualquier capa.
 * El token ya fue validado por JwtFilter; aquí solo se parsea.
 *
 * Uso:
 *   Long userId = JwtUtil.extractUserId(request.getHeader("Authorization"));
 */
@Component
public class JwtUtil {

    private static SecretKey SECRET_KEY;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        JwtUtil.SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extrae el userId del claim "userId" dentro del JWT.
     * Acepta "Bearer xxx" o el token desnudo.
     */
    public static Long extractUserId(String bearerToken) {
        if (bearerToken == null) throw new AppException(AppCode.AUTH_TOKEN_MISSING);
        String token = bearerToken.startsWith("Bearer ")
                ? bearerToken.substring(7) : bearerToken;
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY).build()
                .parseClaimsJws(token).getBody();
        Number userId = (Number) claims.get("userId");
        if (userId == null) throw new AppException(AppCode.AUTH_TOKEN_INVALID);
        return userId.longValue();
    }

    public static String extractUsername(String token) {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}