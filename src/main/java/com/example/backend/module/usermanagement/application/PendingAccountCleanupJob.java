package com.example.backend.module.usermanagement.application;

import com.example.backend.module.usermanagement.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Borra periódicamente las cuentas que se registraron pero nunca verificaron
 * el OTP dentro del plazo. Evita que la tabla users acumule registros
 * huérfanos indefinidamente y libera el username/email/dni para que la
 * persona pueda volver a registrarse.
 *
 * Umbral: 24h — bastante más holgado que los 10 min de vida del OTP, para no
 * borrar una cuenta que el usuario todavía podría reintentar verificar tras
 * pedir uno nuevo (una vez exista el endpoint de reenvío de OTP).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingAccountCleanupJob {

    private static final long STALE_AFTER_HOURS = 24;

    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 3 * * *") // 3:00 AM todos los días
    @Transactional
    public void purgeStalePendingAccounts() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(STALE_AFTER_HOURS);
        int deleted = userRepository.deleteStalePendingVerification(cutoff);
        if (deleted > 0) {
            log.info("[PendingAccountCleanupJob] {} cuenta(s) PENDING_VERIFICATION abandonadas eliminadas (creadas antes de {})",
                    deleted, cutoff);
        }
    }
}
