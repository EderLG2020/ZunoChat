package com.example.backend.common.service;

import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.AppCode;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.security.GeneralSecurityException;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Intercambia el authorization code que devuelve Google Identity Services
 * (modo popup, initCodeClient) por los datos verificados del usuario.
 *
 * No expone tokens de Google al frontend en ningún momento: el code viaja
 * del navegador al backend, y de acá en adelante todo pasa server-to-server.
 */
@Service
public class GoogleOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthClient.class);

    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";

    /**
     * Valor especial que exige el token endpoint de Google cuando el code se
     * obtuvo con ux_mode:'popup' en el frontend — no hay una URL de redirect
     * real, el code vuelve al JS vía postMessage en vez de una navegación.
     */
    private static final String REDIRECT_URI = "postmessage";

    private final String clientId;
    private final String clientSecret;
    private final RestClient restClient;
    private final GoogleIdTokenVerifier verifier;

    public GoogleOAuthClient(@Value("${google.oauth.client-id}") String clientId,
                              @Value("${google.oauth.client-secret}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restClient = RestClient.create();
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public record GoogleProfile(String googleId, String email, boolean emailVerified, String name) {}

    /**
     * Paso único: code → tokens (Google) → id_token verificado → perfil.
     * Cualquier fallo (code inválido/expirado/ya usado, firma inválida,
     * audience/issuer que no matchean) se traduce a GOOGLE_AUTH_INVALID_CODE
     * — no hace falta distinguirlos para el cliente.
     */
    public GoogleProfile exchangeCode(String code) {
        String idTokenString = requestIdToken(code);
        GoogleIdToken idToken = verifyIdToken(idTokenString);

        GoogleIdToken.Payload payload = idToken.getPayload();
        String name = (String) payload.get("name");
        Boolean emailVerified = payload.getEmailVerified();

        return new GoogleProfile(
                payload.getSubject(),
                payload.getEmail(),
                Boolean.TRUE.equals(emailVerified),
                name
        );
    }

    private String requestIdToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", REDIRECT_URI);
        form.add("grant_type", "authorization_code");

        Map<String, Object> tokenResponse;
        try {
            tokenResponse = restClient.post()
                    .uri(TOKEN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientException e) {
            log.warn("Fallo al intercambiar authorization code con Google: {}", e.getMessage());
            throw new AppException(AppCode.GOOGLE_AUTH_INVALID_CODE);
        }

        Object idToken = tokenResponse == null ? null : tokenResponse.get("id_token");
        if (!(idToken instanceof String idTokenString) || idTokenString.isBlank())
            throw new AppException(AppCode.GOOGLE_AUTH_INVALID_CODE);

        return idTokenString;
    }

    private GoogleIdToken verifyIdToken(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null)
                throw new AppException(AppCode.GOOGLE_AUTH_INVALID_CODE);
            return idToken;
        } catch (GeneralSecurityException | IOException e) {
            log.warn("Fallo al verificar id_token de Google: {}", e.getMessage());
            throw new AppException(AppCode.GOOGLE_AUTH_INVALID_CODE);
        }
    }
}
