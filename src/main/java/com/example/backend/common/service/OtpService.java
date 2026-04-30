package com.example.backend.common.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Servicio para generación y validación de códigos OTP de 6 dígitos.
 *
 * En producción, el envío del correo se haría aquí o en un servicio de email
 * (JavaMailSender, SendGrid, AWS SES, etc.).
 */
@Service
public class OtpService {

    /** Tiempo de vida del OTP en minutos */
    private static final int OTP_EXPIRATION_MINUTES = 10;

    private final SecureRandom random = new SecureRandom();

    /**
     * Genera un código OTP de 6 dígitos.
     */
    public String generateOtp() {
        int code = 100_000 + random.nextInt(900_000); // 100000–999999
        return String.valueOf(code);
    }

    /**
     * Calcula la fecha de expiración a partir de ahora.
     */
    public LocalDateTime generateExpiration() {
        return LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES);
    }

    /**
     * Verifica que el OTP coincida y no haya expirado.
     *
     * @param storedOtp      código guardado en BD
     * @param inputOtp       código ingresado por el usuario
     * @param expiration     fecha límite guardada en BD
     */
    public boolean isValid(String storedOtp, String inputOtp, LocalDateTime expiration) {
        if (storedOtp == null || inputOtp == null || expiration == null) return false;
        return storedOtp.equals(inputOtp) && LocalDateTime.now().isBefore(expiration);
    }
}
