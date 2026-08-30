package com.example.backend.module.streak.application;

import com.example.backend.common.enums.ConversationType;
import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.AppCode;
import com.example.backend.module.messagemanagement.domain.ConversationModel;
import com.example.backend.module.messagemanagement.persistence.ConversationRepository;
import com.example.backend.module.streak.domain.StreakCalculator;
import com.example.backend.module.streak.domain.StreakModel;
import com.example.backend.module.streak.domain.StreakRequestStatus;
import com.example.backend.module.streak.domain.StreakStatus;
import com.example.backend.module.streak.dto.StreakResponse;
import com.example.backend.module.streak.persistence.StreakRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Lógica de negocio de la racha (streak) entre dos usuarios.
 *
 * Por defecto NINGUNA conversación cuenta racha — es opt-in mutuo: cualquiera
 * de los dos la activa desde Configuración (requestActivation), lo que envía
 * una solicitud dentro del chat; la racha solo empieza a sumar cuando el otro
 * también acepta (respondToActivation), igual que en Snapchat. Desactivarla,
 * en cambio, es unilateral e inmediato (disable) — no requiere confirmación.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreakService {

    private final StreakRepository streakRepository;
    private final ConversationRepository conversationRepository;
    private final StreakEventPublisher streakEventPublisher;

    // ─── Registrar interacción (llamado desde MessageService#sendMessage) ────

    /**
     * No debe nunca propagar una excepción hacia el flujo de envío de
     * mensajes — un fallo acá (deadlock de lock, fila en estado inesperado,
     * lo que sea) jamás debe impedir que el mensaje ya guardado se devuelva
     * al cliente. Transacción propia (REQUIRES_NEW): el mensaje ya se guardó
     * en la transacción del caller: esto no debe extenderla ni abortarla.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordInteraction(Long conversationId, Long senderId, Long receiverId) {
        if (receiverId == null) return; // GROUP no tiene racha (ver MessageService#sendMessage)
        try {
            StreakModel streak = streakRepository.findByConversationIdForUpdate(conversationId).orElse(null);
            if (streak == null || !streak.isEnabled()) return;

            LocalDate today = LocalDate.now(ZoneOffset.UTC);

            StreakCalculator.Result result = StreakCalculator.apply(new StreakCalculator.Input(
                    streak.getCurrentCount(), streak.getLongestCount(),
                    streak.getLastInteractionDate(),
                    streak.getLastMessageDateA(), streak.getLastMessageDateB(),
                    streak.getUserAId(), streak.getUserBId(),
                    senderId, today
            ));

            streak.setCurrentCount(result.currentCount());
            streak.setLongestCount(result.longestCount());
            streak.setLastInteractionDate(result.lastInteractionDate());
            streak.setLastMessageDateA(result.lastMessageDateA());
            streak.setLastMessageDateB(result.lastMessageDateB());

            if (result.changeType() != StreakCalculator.ChangeType.NONE) {
                streak.setStatus(StreakStatus.ACTIVE);
            }

            streakRepository.save(streak);

            if (result.changeType() == StreakCalculator.ChangeType.INCREMENT) {
                streakEventPublisher.publish("INCREMENTED", streak);
            } else if (result.changeType() == StreakCalculator.ChangeType.RESET) {
                streakEventPublisher.publish("RESET", streak);
            }
        } catch (Exception e) {
            log.error("[StreakService] No se pudo actualizar la racha de la conversación {}: {}",
                    conversationId, e.getMessage(), e);
        }
    }

    // ─── Consulta ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StreakResponse getStreak(Long userId, Long conversationId) {
        ConversationModel conv = getDirectConversationAndVerifyParticipant(conversationId, userId);
        StreakModel streak = streakRepository.findByConversationId(conv.getId()).orElse(null);
        return toResponse(conv.getId(), streak);
    }

    // ─── Activar (solicitar) ────────────────────────────────────────────────

    @Transactional
    public StreakResponse requestActivation(Long userId, Long conversationId) {
        ConversationModel conv = getDirectConversationAndVerifyParticipant(conversationId, userId);
        StreakModel streak = streakRepository.findByConversationId(conv.getId()).orElseGet(() ->
                StreakModel.builder()
                        .conversationId(conv.getId())
                        .userAId(Math.min(conv.getUser1Id(), conv.getUser2Id()))
                        .userBId(Math.max(conv.getUser1Id(), conv.getUser2Id()))
                        .build());

        if (streak.isEnabled()) {
            return toResponse(conv.getId(), streak); // ya estaba activa, no-op
        }

        // El otro ya la había pedido antes → se toma como aceptación mutua
        // sin exigir un segundo paso explícito (evita fricción si los dos
        // prendieron el switch casi al mismo tiempo).
        if (streak.getRequestStatus() == StreakRequestStatus.PENDING
                && streak.getRequestedByUserId() != null
                && !streak.getRequestedByUserId().equals(userId)) {
            streak.setEnabled(true);
            streak.setRequestStatus(StreakRequestStatus.ACCEPTED);
            StreakModel saved = streakRepository.save(streak);
            streakEventPublisher.publish("REQUEST_ACCEPTED", saved);
            return toResponse(conv.getId(), saved);
        }

        streak.setRequestStatus(StreakRequestStatus.PENDING);
        streak.setRequestedByUserId(userId);
        StreakModel saved = streakRepository.save(streak);
        streakEventPublisher.publish("REQUEST_SENT", saved);
        return toResponse(conv.getId(), saved);
    }

    // ─── Responder solicitud ─────────────────────────────────────────────────

    @Transactional
    public StreakResponse respondToActivation(Long userId, Long conversationId, boolean accept) {
        ConversationModel conv = getDirectConversationAndVerifyParticipant(conversationId, userId);
        StreakModel streak = streakRepository.findByConversationId(conv.getId())
                .orElseThrow(() -> new AppException(AppCode.STREAK_NO_PENDING_REQUEST));

        if (streak.getRequestStatus() != StreakRequestStatus.PENDING)
            throw new AppException(AppCode.STREAK_NO_PENDING_REQUEST);
        if (streak.getRequestedByUserId() != null && streak.getRequestedByUserId().equals(userId))
            throw new AppException(AppCode.STREAK_OWN_REQUEST);

        if (accept) {
            streak.setEnabled(true);
            streak.setRequestStatus(StreakRequestStatus.ACCEPTED);
        } else {
            streak.setEnabled(false);
            streak.setRequestStatus(StreakRequestStatus.DECLINED);
        }

        StreakModel saved = streakRepository.save(streak);
        streakEventPublisher.publish(accept ? "REQUEST_ACCEPTED" : "REQUEST_DECLINED", saved);
        return toResponse(conv.getId(), saved);
    }

    // ─── Desactivar ───────────────────────────────────────────────────────────

    /** Unilateral e inmediato: cualquiera de los dos participantes puede apagarla sin que el otro confirme. */
    @Transactional
    public StreakResponse disable(Long userId, Long conversationId) {
        ConversationModel conv = getDirectConversationAndVerifyParticipant(conversationId, userId);
        StreakModel streak = streakRepository.findByConversationId(conv.getId()).orElse(null);
        if (streak == null || !streak.isEnabled()) {
            return toResponse(conv.getId(), streak); // ya estaba apagada
        }

        streak.setEnabled(false);
        streak.setRequestStatus(StreakRequestStatus.NONE);
        streak.setRequestedByUserId(null);
        streak.setStatus(StreakStatus.INACTIVE);
        StreakModel saved = streakRepository.save(streak);
        streakEventPublisher.publish("DISABLED", saved);
        return toResponse(conv.getId(), saved);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ConversationModel getDirectConversationAndVerifyParticipant(Long conversationId, Long userId) {
        ConversationModel conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(AppCode.CONV_NOT_FOUND));

        if (conv.getType() != ConversationType.DIRECT)
            throw new AppException(AppCode.STREAK_NOT_DIRECT);
        if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId))
            throw new AppException(AppCode.AUTH_FORBIDDEN);

        return conv;
    }

    private StreakResponse toResponse(Long conversationId, StreakModel streak) {
        if (streak == null) {
            return new StreakResponse(conversationId, false, 0, 0, null,
                    StreakStatus.INACTIVE, StreakRequestStatus.NONE, null);
        }
        return new StreakResponse(
                conversationId, streak.isEnabled(), streak.getCurrentCount(), streak.getLongestCount(),
                streak.getLastInteractionDate(), streak.getStatus(), streak.getRequestStatus(),
                streak.getRequestedByUserId()
        );
    }
}
