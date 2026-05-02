package com.example.backend.module.messagemanagement.application;

import com.example.backend.common.enums.ConversationStatus;
import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.AppCode;
import com.example.backend.module.messagemanagement.domain.ConversationModel;
import com.example.backend.module.messagemanagement.dto.ConversationResponse;
import com.example.backend.module.messagemanagement.dto.CreateConversationRequest;
import com.example.backend.module.messagemanagement.persistence.ConversationRepository;
import com.example.backend.module.usermanagement.domain.UserModel;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio para conversaciones.
 *
 * Invariante: user1Id siempre es el menor → garantiza unicidad sin duplicados.
 */
@Service
public class ConversationService {

    @Autowired private ConversationRepository conversationRepository;
    @Autowired private UserRepository         userRepository;

    // ─── Listado ──────────────────────────────────────────────────────────────

    /**
     * Lista todas las conversaciones del usuario autenticado.
     * Orden: última actividad DESC (como WhatsApp).
     * Sin JOIN: todos los campos vienen de la tabla conversations.
     */
    @Transactional(readOnly = true)
    public Page<ConversationResponse> listConversations(Long userId, int page, int size) {
        Page<ConversationModel> conversations =
                conversationRepository.findAllByUserId(userId, PageRequest.of(page, size));

        return conversations.map(c -> toResponse(c, userId));
    }

    // ─── Crear conversación ───────────────────────────────────────────────────

    /**
     * Crea una conversación entre el usuario autenticado y targetUserId.
     * Si ya existe, la retorna sin duplicar.
     */
    @Transactional
    public ConversationResponse createOrGet(Long currentUserId, CreateConversationRequest req) {
        Long targetId = req.targetUserId();

        if (currentUserId.equals(targetId))
            throw new AppException(AppCode.CONV_SELF_CONVERSATION);

        // Normalizar: user1 siempre es el menor
        Long u1 = Math.min(currentUserId, targetId);
        Long u2 = Math.max(currentUserId, targetId);

        // Si ya existe, retornarla
        return conversationRepository.findByParticipants(u1, u2)
                .map(c -> toResponse(c, currentUserId))
                .orElseGet(() -> {
                    UserModel user1 = userRepository.findById(u1)
                            .orElseThrow(() -> new AppException(AppCode.USER_NOT_FOUND));
                    UserModel user2 = userRepository.findById(u2)
                            .orElseThrow(() -> new AppException(AppCode.USER_NOT_FOUND));

                    ConversationModel conv = ConversationModel.builder()
                            .user1Id(u1)
                            .user2Id(u2)
                            .user1Username(user1.getUsername())
                            .user2Username(user2.getUsername())
                            .user1Avatar(null) // se actualizará cuando el user suba avatar
                            .user2Avatar(null)
                            .status(ConversationStatus.OFFLINE)
                            .build();

                    return toResponse(conversationRepository.save(conv), currentUserId);
                });
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private ConversationResponse toResponse(ConversationModel c, Long currentUserId) {
        boolean isUser1 = c.getUser1Id().equals(currentUserId);

        Long   otherId       = isUser1 ? c.getUser2Id()       : c.getUser1Id();
        String otherUsername = isUser1 ? c.getUser2Username()  : c.getUser1Username();
        String otherAvatar   = isUser1 ? c.getUser2Avatar()    : c.getUser1Avatar();
        int    unread        = isUser1 ? c.getUnreadCountUser1(): c.getUnreadCountUser2();
        boolean isMine       = currentUserId.equals(c.getLastMessageSenderId());

        return new ConversationResponse(
                c.getId(),
                otherId,
                otherUsername,
                otherAvatar,
                c.getLastMessagePreview(),
                isMine,
                c.getLastMessageAt(),
                c.getStatus(),
                unread
        );
    }
}