package com.example.backend.common.util;

import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.AppCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utilidad estática para leer datos del usuario autenticado en cualquier capa.
 *
 * El JWT ya fue validado y parseado una única vez por JwtFilter, que deja
 * el userId en Authentication#getDetails(). Aquí solo se lee ese valor —
 * no se vuelve a verificar la firma del token en cada llamada.
 *
 * Uso:
 *   Long userId = JwtUtil.currentUserId();
 */
@Component
public class JwtUtil {

    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new AppException(AppCode.AUTH_TOKEN_MISSING);

        Object details = auth.getDetails();
        if (details instanceof Long l) return l;
        if (details instanceof Number n) return n.longValue();
        throw new AppException(AppCode.AUTH_TOKEN_INVALID);
    }

    public static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new AppException(AppCode.AUTH_TOKEN_MISSING);
        return auth.getName();
    }
}
