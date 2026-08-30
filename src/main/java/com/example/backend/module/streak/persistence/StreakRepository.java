package com.example.backend.module.streak.persistence;

import com.example.backend.module.streak.domain.StreakModel;
import com.example.backend.module.streak.domain.StreakStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StreakRepository extends JpaRepository<StreakModel, Long> {

    Optional<StreakModel> findByConversationId(Long conversationId);

    /**
     * Igual que findByConversationId, pero toma un lock de escritura hasta
     * que termine la transacción — evita el "lost update" cuando dos
     * mensajes casi simultáneos (uno de cada lado) compiten por completar
     * el mismo día mutuo a la vez (ver StreakService#recordInteraction).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StreakModel s WHERE s.conversationId = :conversationId")
    Optional<StreakModel> findByConversationIdForUpdate(@Param("conversationId") Long conversationId);

    /** Usado por StreakExpiryScheduler para clasificar rachas activas o en riesgo. */
    List<StreakModel> findAllByEnabledTrueAndStatus(StreakStatus status);
}
