package com.example.backend.module.messagemanagement.application;

import com.example.backend.common.enums.MessageStatus;
import com.example.backend.common.enums.MessageType;
import com.example.backend.common.enums.PayloadType;
import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.AppCode;
import com.example.backend.module.messagemanagement.domain.ConversationModel;
import com.example.backend.module.messagemanagement.domain.MessageModel;
import com.example.backend.module.messagemanagement.dto.MessageCursorPage;
import com.example.backend.module.messagemanagement.dto.MessageResponse;
import com.example.backend.module.messagemanagement.dto.SendMessageRequest;
import com.example.backend.module.messagemanagement.persistence.ConversationRepository;
import com.example.backend.module.messagemanagement.persistence.MessageRepository;
import com.example.backend.module.messagemanagement.realtime.session.IWebSocketSessionRegistry;
import com.example.backend.module.usermanagement.persistence.BlockedUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class MessageService {

    /** Ventana para poder editar un mensaje de texto después de enviado. */
    private static final long EDIT_WINDOW_MINUTES = 15;

    @Autowired private MessageRepository           messageRepository;
    @Autowired private ConversationRepository      conversationRepository;
    @Autowired private IWebSocketSessionRegistry   sessionRegistry;
    @Autowired private BlockedUserRepository       blockedUserRepository;

    // ─── Listar mensajes ─────────────────────────────────────────────────────

    /**
     * beforeId = null → los `size` mensajes más recientes de la conversación.
     * beforeId != null → los `size` mensajes inmediatamente anteriores a ese id.
     * Pide size+1 filas para saber si hay más sin necesitar un COUNT(*) aparte.
     */
    @Transactional(readOnly = true)
    public MessageCursorPage listMessages(Long conversationId, Long currentUserId,
                                          Long beforeId, int size) {
        getConversationAndVerifyParticipant(conversationId, currentUserId);

        List<MessageModel> rows = messageRepository.findPageByConversationId(
                conversationId, beforeId, PageRequest.of(0, size + 1));

        boolean hasMore = rows.size() > size;
        List<MessageModel> page = hasMore ? rows.subList(0, size) : rows;
        Long nextCursor = page.isEmpty() ? null : page.get(page.size() - 1).getId();

        return new MessageCursorPage(page.stream().map(this::toResponse).toList(), hasMore, nextCursor);
    }

    // ─── Enviar mensaje (REST + WS comparten este método) ────────────────────

    @Transactional
    public MessageResponse sendMessage(Long senderId, SendMessageRequest req) {
        ConversationModel conv = getConversationAndVerifyParticipant(req.conversationId(), senderId);

        Long receiverId = conv.getUser1Id().equals(senderId)
                ? conv.getUser2Id() : conv.getUser1Id();

        if (blockedUserRepository.existsBetween(senderId, receiverId))
            throw new AppException(AppCode.USER_BLOCKED_CONTACT);

        validateMessage(req);

        // Se resuelve el status ANTES de construir la entidad para no pagar
        // un segundo INSERT/UPDATE cuando el receptor ya está conectado (el
        // caso más común) — antes se guardaba como SENT y, si correspondía,
        // se volvía a guardar como DELIVERED en un segundo round-trip a BD.
        MessageStatus initialStatus = sessionRegistry.hasActiveSessions(receiverId)
                ? MessageStatus.DELIVERED : MessageStatus.SENT;

        MessageModel msg = MessageModel.builder()
                .conversationId(req.conversationId())
                .senderId(senderId)
                .receiverId(receiverId)
                .type(req.type())
                .textContent(req.textContent())
                .payload(req.payload())
                .payloadType(req.payloadType())
                .fileUrls(req.fileUrls())
                .status(initialStatus)
                .build();

        MessageModel saved = messageRepository.save(msg);

        updateConversationPreview(conv, senderId, req, saved.getSentAt());

        return toResponse(saved);
    }

    // ─── Marcar como leído ────────────────────────────────────────────────────

    @Transactional
    public int markAsRead(Long conversationId, Long currentUserId) {
        ConversationModel conv = getConversationAndVerifyParticipant(conversationId, currentUserId);

        int updated = messageRepository.markAsRead(conversationId, currentUserId, MessageStatus.READ);

        if (conv.getUser1Id().equals(currentUserId)) {
            conversationRepository.resetUnreadUser1(conversationId);
        } else {
            conversationRepository.resetUnreadUser2(conversationId);
        }

        return updated;
    }

    // ─── Borrar mensaje (soft delete) ─────────────────────────────────────────

    @Transactional
    public MessageResponse deleteMessage(Long messageId, Long currentUserId) {
        MessageModel msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(AppCode.MSG_NOT_FOUND));

        if (!msg.getSenderId().equals(currentUserId))
            throw new AppException(AppCode.MSG_NOT_OWNER);
        if (msg.isDeleted())
            throw new AppException(AppCode.MSG_ALREADY_DELETED);

        msg.setDeleted(true);
        msg.setTextContent(null);
        msg.setPayload(null);
        msg.setFileUrls(null);
        MessageModel saved = messageRepository.save(msg);

        return toResponse(saved);
    }

    // ─── Editar mensaje ────────────────────────────────────────────────────────

    @Transactional
    public MessageResponse editMessage(Long messageId, Long currentUserId, String newText) {
        MessageModel msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(AppCode.MSG_NOT_FOUND));

        if (!msg.getSenderId().equals(currentUserId))
            throw new AppException(AppCode.MSG_NOT_OWNER);
        if (msg.isDeleted())
            throw new AppException(AppCode.MSG_ALREADY_DELETED);
        if (msg.getType() != MessageType.TEXT)
            throw new AppException(AppCode.MSG_EDIT_NOT_TEXT);
        if (msg.getSentAt().plus(EDIT_WINDOW_MINUTES, ChronoUnit.MINUTES).isBefore(LocalDateTime.now()))
            throw new AppException(AppCode.MSG_EDIT_WINDOW_EXPIRED);
        if (newText == null || newText.isBlank())
            throw new AppException(AppCode.MSG_TEXT_REQUIRED);

        msg.setTextContent(newText);
        msg.setEditedAt(LocalDateTime.now());
        MessageModel saved = messageRepository.save(msg);

        return toResponse(saved);
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private ConversationModel getConversationAndVerifyParticipant(Long convId, Long userId) {
        ConversationModel conv = conversationRepository.findById(convId)
                .orElseThrow(() -> new AppException(AppCode.CONV_NOT_FOUND));
        if (!conv.getUser1Id().equals(userId) && !conv.getUser2Id().equals(userId))
            throw new AppException(AppCode.AUTH_FORBIDDEN);
        return conv;
    }

    private void validateMessage(SendMessageRequest req) {
        if (req.type() == MessageType.TEXT && (req.textContent() == null || req.textContent().isBlank()))
            throw new AppException(AppCode.MSG_TEXT_REQUIRED);
        if (req.type() == MessageType.PAYLOAD && req.payload() == null)
            throw new AppException(AppCode.MSG_PAYLOAD_REQUIRED);
        if (req.type() == MessageType.FILE && (req.fileUrls() == null || req.fileUrls().isEmpty()))
            throw new AppException(AppCode.MSG_FILE_REQUIRED);
        if (req.fileUrls() != null && req.fileUrls().size() > 3)
            throw new AppException(AppCode.MSG_FILE_LIMIT);
    }

    private void updateConversationPreview(ConversationModel conv, Long senderId,
                                           SendMessageRequest req, LocalDateTime sentAt) {
        String preview = buildPreview(req);
        boolean senderIsUser1 = conv.getUser1Id().equals(senderId);
        if (senderIsUser1) {
            conversationRepository.applyNewMessageFromUser1(conv.getId(), preview, senderId, sentAt);
        } else {
            conversationRepository.applyNewMessageFromUser2(conv.getId(), preview, senderId, sentAt);
        }
    }

    private String buildPreview(SendMessageRequest req) {
        String raw = switch (req.type()) {
            case TEXT    -> req.textContent() != null ? req.textContent() : "";
            case IMAGE   -> "📷 Imagen";
            case FILE    -> "📎 Archivo adjunto";
            case PAYLOAD -> switch (req.payloadType() != null ? req.payloadType() : PayloadType.SYSTEM) {
                case SALES  -> "🛒 Oferta";
                case SYSTEM -> "⚙️ Notificación";
                case SURVEY -> "📋 Encuesta";
                case CARD   -> "🃏 Tarjeta";
            };
        };
        return raw.length() > 50 ? raw.substring(0, 47) + "..." : raw;
    }

    private MessageResponse toResponse(MessageModel m) {
        return new MessageResponse(
                m.getId(), m.getConversationId(),
                m.getSenderId(), m.getReceiverId(),
                m.getType(), m.getTextContent(),
                m.getPayload(), m.getPayloadType(),
                m.getFileUrls(), m.getStatus(),
                m.getSentAt(), m.getReadAt(),
                m.isDeleted(), m.getEditedAt()
        );
    }
}