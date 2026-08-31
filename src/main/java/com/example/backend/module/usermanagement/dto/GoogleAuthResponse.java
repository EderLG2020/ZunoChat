package com.example.backend.module.usermanagement.dto;

/**
 * Respuesta de POST /api/auth/google.
 *
 * needsUsername = false → cuenta ya existía (o se acaba de vincular por
 *   email verificado): "auth" viene lleno, igual que login/verify-otp.
 * needsUsername = true  → cuenta nueva: "auth" es null, hay que llamar a
 *   POST /api/auth/google/complete con "registrationToken" + el username
 *   elegido por el usuario. "suggestedUsername" es solo un placeholder de
 *   UI (derivado del email), no está garantizado que esté libre.
 */
public record GoogleAuthResponse(

        boolean needsUsername,
        AuthResponse auth,
        String registrationToken,
        String email,
        String suggestedUsername

) {
    public static GoogleAuthResponse loggedIn(AuthResponse auth) {
        return new GoogleAuthResponse(false, auth, null, null, null);
    }

    public static GoogleAuthResponse pendingUsername(String registrationToken, String email, String suggestedUsername) {
        return new GoogleAuthResponse(true, null, registrationToken, email, suggestedUsername);
    }
}
