package com.example.backend.module.streak.application;

import com.example.backend.module.streak.domain.StreakModel;
import com.example.backend.module.streak.domain.StreakStatus;
import com.example.backend.module.streak.persistence.StreakRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Corre una vez al día, justo después de medianoche UTC, y reclasifica las
 * rachas activas según si hubo interacción mutua el día que acaba de
 * terminar (ver StreakService#recordInteraction para cómo se mueve
 * lastInteractionDate).
 *
 * ACTIVE  → AT_RISK: la pareja interactuó ayer (UTC) pero todavía no hoy —
 *           la racha sigue viva pero se pierde si ninguno escribe antes de
 *           la próxima medianoche.
 * (ACTIVE o AT_RISK) → BROKEN: no hubo interacción mutua ni ayer — la racha
 *           se corta; currentCount vuelve a 0 (longestCount se conserva).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreakExpiryScheduler {

    private final StreakRepository streakRepository;
    private final StreakEventPublisher streakEventPublisher;

    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    @Transactional
    public void sweepStreaks() {
        LocalDate today     = LocalDate.now(ZoneOffset.UTC);
        LocalDate yesterday = today.minusDays(1);

        int atRiskCount = 0;
        int brokenCount = 0;

        for (StreakModel streak : streakRepository.findAllByEnabledTrueAndStatus(StreakStatus.ACTIVE)) {
            if (yesterday.equals(streak.getLastInteractionDate())) {
                streak.setStatus(StreakStatus.AT_RISK);
                streakRepository.save(streak);
                streakEventPublisher.publish("AT_RISK", streak);
                atRiskCount++;
            } else if (staleSince(streak, yesterday)) {
                breakStreak(streak);
                brokenCount++;
            }
        }

        // Rachas que ya estaban AT_RISK desde el corte anterior: si nadie
        // escribió mientras tanto, hoy se cortan definitivamente.
        for (StreakModel streak : streakRepository.findAllByEnabledTrueAndStatus(StreakStatus.AT_RISK)) {
            if (staleSince(streak, yesterday)) {
                breakStreak(streak);
                brokenCount++;
            }
        }

        if (atRiskCount > 0 || brokenCount > 0) {
            log.info("[StreakExpiryScheduler] {} racha(s) marcadas AT_RISK, {} marcadas BROKEN", atRiskCount, brokenCount);
        }
    }

    private boolean staleSince(StreakModel streak, LocalDate yesterday) {
        return streak.getLastInteractionDate() == null || streak.getLastInteractionDate().isBefore(yesterday);
    }

    private void breakStreak(StreakModel streak) {
        streak.setStatus(StreakStatus.BROKEN);
        streak.setCurrentCount(0);
        streakRepository.save(streak);
        streakEventPublisher.publish("BROKEN", streak);
    }
}
