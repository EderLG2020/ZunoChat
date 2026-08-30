package com.example.backend.module.streak.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StreakCalculatorTest {

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 29);

    private StreakCalculator.Input input(int currentCount, int longestCount, LocalDate lastInteractionDate,
                                          LocalDate lastMessageDateA, LocalDate lastMessageDateB, Long senderId) {
        return new StreakCalculator.Input(
                currentCount, longestCount, lastInteractionDate,
                lastMessageDateA, lastMessageDateB,
                USER_A, USER_B, senderId, TODAY
        );
    }

    @Test
    void mismoDia_yaContado_noVuelveAIncrementar() {
        // Ambos ya habían escrito hoy y el día ya quedó contado; llega un tercer mensaje.
        StreakCalculator.Result result = StreakCalculator.apply(
                input(4, 4, TODAY, TODAY, TODAY, USER_A));

        assertThat(result.changeType()).isEqualTo(StreakCalculator.ChangeType.NONE);
        assertThat(result.currentCount()).isEqualTo(4);
        assertThat(result.longestCount()).isEqualTo(4);
        assertThat(result.lastInteractionDate()).isEqualTo(TODAY);
    }

    @Test
    void diaConsecutivo_incrementaContador() {
        // Último día mutuo contado fue ayer; A ya había escrito hoy, ahora escribe B → se completa el día.
        StreakCalculator.Result result = StreakCalculator.apply(
                input(3, 5, TODAY.minusDays(1), TODAY, TODAY.minusDays(1), USER_B));

        assertThat(result.changeType()).isEqualTo(StreakCalculator.ChangeType.INCREMENT);
        assertThat(result.currentCount()).isEqualTo(4);
        assertThat(result.longestCount()).isEqualTo(5); // no baja el récord aunque el actual sea menor
        assertThat(result.lastInteractionDate()).isEqualTo(TODAY);
    }

    @Test
    void gapDeMasDeUnDia_reiniciaA1() {
        // Última interacción mutua fue hace 3 días — se rompió la racha.
        StreakCalculator.Result result = StreakCalculator.apply(
                input(10, 10, TODAY.minusDays(3), TODAY, TODAY.minusDays(3), USER_B));

        assertThat(result.changeType()).isEqualTo(StreakCalculator.ChangeType.RESET);
        assertThat(result.currentCount()).isEqualTo(1);
        assertThat(result.longestCount()).isEqualTo(10); // el récord histórico no se pierde
        assertThat(result.lastInteractionDate()).isEqualTo(TODAY);
    }

    @Test
    void primeraInteraccionMutua_arrancaEn1() {
        // Nunca hubo un día mutuo registrado; A escribe hoy y B también.
        StreakCalculator.Result result = StreakCalculator.apply(
                input(0, 0, null, TODAY, TODAY, USER_B));

        assertThat(result.changeType()).isEqualTo(StreakCalculator.ChangeType.RESET);
        assertThat(result.currentCount()).isEqualTo(1);
        assertThat(result.longestCount()).isEqualTo(1);
        assertThat(result.lastInteractionDate()).isEqualTo(TODAY);
    }

    @Test
    void soloUnoEscribioHoy_noCompletaElDia() {
        // Solo A escribió hoy; B todavía no — el día mutuo no se completa,
        // pero sí queda registrado que A ya escribió.
        StreakCalculator.Result result = StreakCalculator.apply(
                input(2, 2, TODAY.minusDays(1), null, TODAY.minusDays(1), USER_A));

        assertThat(result.changeType()).isEqualTo(StreakCalculator.ChangeType.NONE);
        assertThat(result.currentCount()).isEqualTo(2);
        assertThat(result.lastInteractionDate()).isEqualTo(TODAY.minusDays(1));
        assertThat(result.lastMessageDateA()).isEqualTo(TODAY);
        assertThat(result.lastMessageDateB()).isEqualTo(TODAY.minusDays(1));
    }
}
