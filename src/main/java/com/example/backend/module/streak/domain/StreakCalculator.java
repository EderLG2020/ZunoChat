package com.example.backend.module.streak.domain;

import java.time.LocalDate;

/**
 * Lógica pura de cálculo de racha — sin Spring ni JPA, para poder testearla
 * sin levantar contexto. Regla de negocio: un día (UTC) solo cuenta para la
 * racha cuando AMBOS usuarios enviaron al menos un mensaje ese mismo día.
 *
 * Casos (ver StreakCalculatorTest):
 *  - Ya estaba contado hoy               → NONE, sin cambios.
 *  - Solo uno de los dos escribió hoy    → NONE, solo se actualiza su lastMessageDate*.
 *  - Último día mutuo = ayer             → INCREMENT (currentCount + 1).
 *  - Último día mutuo = hoy - 2 o más, o nunca hubo (primera interacción) → RESET (currentCount = 1).
 */
public final class StreakCalculator {

    private StreakCalculator() {}

    public enum ChangeType { NONE, INCREMENT, RESET }

    public record Input(
            int currentCount,
            int longestCount,
            LocalDate lastInteractionDate,
            LocalDate lastMessageDateA,
            LocalDate lastMessageDateB,
            Long userAId,
            Long userBId,
            Long senderId,
            LocalDate today
    ) {}

    public record Result(
            int currentCount,
            int longestCount,
            LocalDate lastInteractionDate,
            LocalDate lastMessageDateA,
            LocalDate lastMessageDateB,
            ChangeType changeType
    ) {}

    public static Result apply(Input in) {
        LocalDate newA = in.userAId().equals(in.senderId()) ? in.today() : in.lastMessageDateA();
        LocalDate newB = in.userBId().equals(in.senderId()) ? in.today() : in.lastMessageDateB();

        boolean bothWroteToday = in.today().equals(newA) && in.today().equals(newB);

        // Solo uno de los dos escribió hoy (o ninguno) — el día mutuo todavía
        // no se completa, pero sí queda registrado quién ya escribió.
        if (!bothWroteToday) {
            return new Result(in.currentCount(), in.longestCount(), in.lastInteractionDate(), newA, newB, ChangeType.NONE);
        }

        // Ya se había contado este mismo día (un tercer mensaje del mismo día no vuelve a incrementar).
        if (in.today().equals(in.lastInteractionDate())) {
            return new Result(in.currentCount(), in.longestCount(), in.lastInteractionDate(), newA, newB, ChangeType.NONE);
        }

        // Día consecutivo: el último día mutuo contado fue justo ayer.
        if (in.lastInteractionDate() != null && in.lastInteractionDate().equals(in.today().minusDays(1))) {
            int newCount = in.currentCount() + 1;
            int newLongest = Math.max(in.longestCount(), newCount);
            return new Result(newCount, newLongest, in.today(), newA, newB, ChangeType.INCREMENT);
        }

        // Gap de más de un día, o primera interacción mutua registrada — arranca de 1.
        int newCount = 1;
        int newLongest = Math.max(in.longestCount(), newCount);
        return new Result(newCount, newLongest, in.today(), newA, newB, ChangeType.RESET);
    }
}
