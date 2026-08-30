package com.example.backend.module.streak.domain;

/**
 * INACTIVE: nunca activada, o desactivada manualmente por alguno de los dos.
 * ACTIVE:   racha viva — hubo interacción mutua hoy o ayer (UTC).
 * AT_RISK:  hubo interacción mutua ayer (UTC) pero todavía no hoy — se pierde si nadie escribe antes de medianoche.
 * BROKEN:   pasó más de un día (UTC) sin interacción mutua — ver StreakExpiryScheduler.
 */
public enum StreakStatus {
    INACTIVE,
    ACTIVE,
    AT_RISK,
    BROKEN
}
