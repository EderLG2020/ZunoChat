package com.example.backend.module.streak.domain;

/**
 * Estado del opt-in mutuo para activar la racha (ver StreakService):
 * NONE:      nadie la pidió (o se desactivó y quedó en este estado neutro).
 * PENDING:   uno de los dos la pidió, falta la respuesta del otro.
 * ACCEPTED:  el otro aceptó — la racha queda enabled=true.
 * DECLINED:  el otro la rechazó — enabled queda en false.
 */
public enum StreakRequestStatus {
    NONE,
    PENDING,
    ACCEPTED,
    DECLINED
}
