package com.example.backend.common.security.filter;

import com.example.backend.common.enums.UserStatus;
import com.example.backend.common.security.IUserStatusCache;
import com.example.backend.common.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtro JWT.
 *
 * Lee el token, extrae el rol y los permisos granulares,
 * y los registra como GrantedAuthority en el SecurityContext.
 *
 * Esto permite usar tanto:
 *   @PreAuthorize("hasRole('ADMIN')")
 * como:
 *   @PreAuthorize("hasAuthority('dashboard:editar')")
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private IUserStatusCache userStatusCache;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // /api/auth/refresh recibe intencionalmente un JWT ya EXPIRADO (esa es
        // su función) — si lo procesáramos aquí como cualquier otro request,
        // el catch de ExpiredJwtException de abajo respondería 401 antes de
        // que el controller pudiera aplicar su propia ventana de gracia.
        if (request.getRequestURI().equals("/api/auth/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            Claims claims = jwtService.extractClaims(token);
            String username = claims.getSubject();

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Revalida contra el estado real del usuario (cacheado 30s, ver
                // UserStatusCache) — sin esto, banear/desactivar a alguien no
                // tenía ningún efecto hasta que su JWT expirara solo (24h).
                UserStatus status = userStatusCache.getStatus(username);
                if (status == UserStatus.BANNED || status == UserStatus.INACTIVE || status == UserStatus.DELETED) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Cuenta suspendida o inactiva\"}");
                    return;
                }

                List<GrantedAuthority> authorities = new ArrayList<>();

                // 1. Agregar el rol con prefijo ROLE_ para hasRole()
                String role = claims.get("role", String.class);
                if (role != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }

                // 2. Agregar permisos granulares para hasAuthority()
                @SuppressWarnings("unchecked")
                List<String> permissions = claims.get("permissions", List.class);
                if (permissions != null) {
                    for (String permission : permissions) {
                        authorities.add(new SimpleGrantedAuthority(permission));
                    }
                }

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                // userId queda disponible en los details → evita volver a parsear
                // y verificar la firma del JWT en cada controller (ver JwtUtil.currentUserId()).
                Number userId = claims.get("userId", Number.class);
                if (userId != null) auth.setDetails(userId.longValue());

                SecurityContextHolder.getContext().setAuthentication(auth);
            }

        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token expirado\"}");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            // Cubre firma inválida, token malformado, vacío, o cualquier otro
            // fallo de parseo — antes solo se atrapaban 2 subtipos puntuales
            // y el resto caía al handler genérico de 500.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token inválido\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
