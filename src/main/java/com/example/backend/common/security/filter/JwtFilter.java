package com.example.backend.common.security.filter;

import com.example.backend.common.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

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

                SecurityContextHolder.getContext().setAuthentication(auth);
            }

        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token expirado\"}");
            return;
        } catch (SignatureException | MalformedJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token inválido\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
