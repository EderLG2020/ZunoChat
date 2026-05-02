package com.example.backend.module.messagemanagement.application;

import com.example.backend.common.enums.MessageStatus;
import com.example.backend.common.enums.MessageType;
import com.example.backend.common.enums.PayloadType;
import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.AppCode;

import com.example.backend.module.messagemanagement.domain.ConversationModel;
import com.example.backend.module.messagemanagement.domain.MessageModel;
import com.example.backend.module.messagemanagement.dto.MessageResponse;
import com.example.backend.module.messagemanagement.dto.SendMessageRequest;
import com.example.backend.module.messagemanagement.persistence.ConversationRepository;
import com.example.backend.module.messagemanagement.persistence.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MessageService {

    @Autowired private MessageRepository messageRepository;
    @Autowired private ConversationRepository conversationRepository;

    // ─── Listar mensajes ─────────────────────────────────────────────────────

    /**
     * Listamensaje de una conversación en orden inverso (más reciente primero).
     * El usuario autenticado debe ser participante de la conversación.
     */
    @Transactional(readOnly = true)
    public Page<MessageResponse> listMessages(Long conversationId, Long currentUserId,
                                              int page, int size) {
        ConversationModel conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(AppCode.CONV_NOT_FOUND));

        // Verificar que el usuario es participante
        if (!conv.getUser1Id().equals(currentUserId) && !conv.getUser2Id().equals(currentUserId))
            throw new AppException(AppCode.AUTH_FORBIDDEN);

        return messageRepository
                .findByConversationId(conversationId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    // ─── Enviar mensaje ──────────────────────────────────────────────────────

    @Transactional
    public MessageResponse sendMessage(Long senderId, SendMessageRequest req) {
        ConversationModel conv = conversationRepository.findById(req.conversationId())
                .orElseThrow(() -> new AppException(AppCode.CONV_NOT_FOUND));

        // Verificar participación
        if (!conv.getUser1Id().equals(senderId) && !conv.getUser2Id().equals(senderId))
            throw new AppException(AppCode.AUTH_FORBIDDEN);

        Long receiverId = conv.getUser1Id().equals(senderId)
                ? conv.getUser2Id() : conv.getUser1Id();

        // Validaciones de negocio
        validateMessage(req);

        MessageModel msg = MessageModel.builder()
                .conversationId(req.conversationId())
                .senderId(senderId)
                .receiverId(receiverId)
                .type(req.type())
                .textContent(req.textContent())
                .payload(req.payload())
                .payloadType(req.payloadType())
                .fileUrls(req.fileUrls())
                .status(MessageStatus.SENT)
                .build();

        MessageModel saved = messageRepository.save(msg);

        // Actualizar desnormalización en conversation (preview + último mensaje)
        updateConversationPreview(conv, senderId, req, saved.getSentAt());

        return toResponse(saved);
    }

    // ─── Marcar como leído (visto) ────────────────────────────────────────────

    /**
     * Marca todos los mensajes recibidos en la conversación como READ.
     * Bulk update → sin cargar entidades en memoria.
     * Resetea el contador de no leídos del usuario autenticado.
     */
    @Transactional
    public int markAsRead(Long conversationId, Long currentUserId) {
        ConversationModel conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(AppCode.CONV_NOT_FOUND));

        if (!conv.getUser1Id().equals(currentUserId) && !conv.getUser2Id().equals(currentUserId))
            throw new AppException(AppCode.AUTH_FORBIDDEN);

        int updated = messageRepository.markAsRead(conversationId, currentUserId, MessageStatus.READ);

        // Resetear contador de no leídos
        if (conv.getUser1Id().equals(currentUserId)) {
            conv.setUnreadCountUser1(0);
        } else {
            conv.setUnreadCountUser2(0);
        }
        conversationRepository.save(conv);

        return updated;
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private void validateMessage(SendMessageRequest req) {
        if (req.type() == MessageType.TEXT && (req.textContent() == null || req.textContent().isBlank()))
            throw new AppException(AppCode.MSG_TEXT_REQUIRED);

        if (req.type() == MessageType.PAYLOAD && req.payload() == null)
            throw new AppException(AppCode.MSG_PAYLOAD_REQUIRED);

        if (req.type() == MessageType.FILE
                && (req.fileUrls() == null || req.fileUrls().isEmpty()))
            throw new AppException(AppCode.MSG_FILE_REQUIRED);

        if (req.fileUrls() != null && req.fileUrls().size() > 3)
            throw new AppException(AppCode.MSG_FILE_LIMIT);
    }

    /**
     * Actualiza el preview del último mensaje en la conversación.
     * Trunca a 50 chars (el frontend muestra 15 visualmente).
     * Incrementa el contador de no leídos del receptor.
     */
    private void updateConversationPreview(ConversationModel conv, Long senderId,
                                           SendMessageRequest req, LocalDateTime sentAt) {
        String preview = buildPreview(req);
        conv.setLastMessagePreview(preview);
        conv.setLastMessageSenderId(senderId);
        conv.setLastMessageAt(sentAt);

        // Incrementar no leídos del receptor
        boolean senderIsUser1 = conv.getUser1Id().equals(senderId);
        if (senderIsUser1) {
            conv.setUnreadCountUser2(conv.getUnreadCountUser2() + 1);
        } else {
            conv.setUnreadCountUser1(conv.getUnreadCountUser1() + 1);
        }
        conversationRepository.save(conv);
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
                m.getSentAt(), m.getReadAt()
        );
    }
}