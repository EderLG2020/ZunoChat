package com.example.backend.common.service;

import com.example.backend.module.usermanagement.domain.UserModel;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Servicio JWT.
 *
 * El token generado contiene:
 * - sub       → username
 * - role      → nombre del rol (USER / ADMIN / SUPERADMIN)
 * - permissions → lista de permisos granulares ["dashboard:editar", ...]
 * - iat / exp
 *
 * En los controllers se usa así:
 *   @PreAuthorize("hasAuthority('dashboard:editar')")
 *
 * La clave secreta se lee de application.properties (jwt.secret).
 * Debe tener al menos 32 caracteres para HS256.
 */
@Service
public class JwtService {

    /** Expiración del token: 24 horas */
    private static final long EXPIRATION_MS = 86_400_000L;

    private final SecretKey secretKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        // Keys.hmacShaKeyFor exige mínimo 256 bits para HS256
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ─── Generación ──────────────────────────────────────────────────────────

    /** Primer login/registro/verify-otp — la sesión empieza AHORA. */
    public String generateToken(UserModel user) {
        return generateToken(user, System.currentTimeMillis());
    }

    /**
     * Usado por /api/auth/refresh para renovar el token PRESERVANDO el
     * momento del primer login (sessionStartMillis) en vez de reiniciarlo.
     * Sin esto, cada refresh reseteaba el reloj y un token robado podía
     * renovarse indefinidamente mientras se refrescara al menos una vez
     * dentro de la ventana de expiración — ver AuthService#refresh.
     */
    public String generateToken(UserModel user, long sessionStartMillis) {
        List<String> permissions = user.getRole().getPermissions();

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("role", user.getRole().name())
                .claim("permissions", permissions)   // ← permisos granulares
                .claim("userId", user.getId())
                .claim("sessionStart", sessionStartMillis)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // ─── Extracción ──────────────────────────────────────────────────────────

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Igual que extractClaims, pero tolera un token EXPIRADO (sigue exigiendo
     * firma válida) — lo usa /api/auth/refresh para renovar un JWT vencido
     * dentro de la ventana de gracia, sin obligar al usuario a loguearse de
     * nuevo. Cualquier otro fallo (firma inválida, malformado) sigue lanzando.
     */
    public Claims extractClaimsAllowExpired(String token) {
        try {
            return extractClaims(token);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        return extractClaims(token).get("permissions", List.class);
    }

    public boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    public boolean isTokenValid(String token, String expectedUsername) {
        return extractUsername(token).equals(expectedUsername) && !isTokenExpired(token);
    }
}
