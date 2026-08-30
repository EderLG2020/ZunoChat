package com.example.backend.module.messagemanagement.realtime.messaging;

import com.example.backend.common.enums.ConversationType;
import com.example.backend.module.messagemanagement.domain.ConversationModel;
import com.example.backend.module.messagemanagement.dto.MessageResponse;
import com.example.backend.module.messagemanagement.persistence.ConversationRepository;
import com.example.backend.module.messagemanagement.persistence.GroupMemberRepository;
import com.example.backend.module.messagemanagement.realtime.messaging.event.MessageEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Construye el MessageEvent para broadcast a partir de una MessageResponse,
 * resolviendo a quién avisarle por /queue/notifications: en DIRECT es el otro
 * participante (desnormalizado en ConversationModel, sin JOIN); en GROUP es
 * cada miembro (ver GroupMemberModel).
 *
 * Antes esta misma lógica (resolver receiverUsername, armar el MessageEvent)
 * estaba duplicada e implementada de forma ligeramente distinta en
 * MessageController.toEvent() y en WebSocketController.sendMessage() — un
 * solo punto de mantenimiento evita que diverjan.
 */
@Component
@RequiredArgsConstructor
public class MessageEventFactory {

    private final ConversationRepository conversationRepository;
    private final GroupMemberRepository  groupMemberRepository;

    /**
     * @param actingUserId   id del usuario que disparó la acción (quien envió/editó/borró)
     * @param actingUsername username de ese mismo usuario (evita re-consultarlo)
     */
    public MessageEvent from(MessageResponse result, Long actingUserId, String actingUsername) {
        ConversationModel conv = conversationRepository.findById(result.conversationId()).orElse(null);

        if (conv != null && conv.getType() == ConversationType.GROUP) {
            List<String> notifyUsernames = groupMemberRepository.findByConversationId(conv.getId())
                    .stream().map(m -> m.getUsername()).distinct().collect(Collectors.toList());

            return new MessageEvent(
                    result.messageId(), result.conversationId(),
                    result.senderId(), actingUsername,
                    null, null,
                    result.type(), result.textContent(),
                    result.payload(), result.payloadType(),
                    result.fileUrls(), result.status(), result.sentAt(),
                    result.deleted(), result.editedAt(), result.expiresAt(),
                    notifyUsernames
            );
        }

        String otherUsername = conv != null
                ? (conv.getUser1Id().equals(actingUserId) ? conv.getUser2Username() : conv.getUser1Username())
                : String.valueOf(result.receiverId());

        boolean actingIsSender = result.senderId().equals(actingUserId);
        String senderUsername = actingIsSender ? actingUsername : otherUsername;
        String receiverUsername = actingIsSender ? otherUsername : actingUsername;

        // Stream+filter (no List.of): actingUsername puede venir null cuando quien
        // dispara el evento es un job del sistema (ver EphemeralMessageSweeper),
        // no un usuario real — y List.of lanza NullPointerException con null.
        List<String> notifyUsernames = Stream.of(senderUsername, receiverUsername)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream().toList();

        return new MessageEvent(
                result.messageId(), result.conversationId(),
                result.senderId(), senderUsername,
                result.receiverId(), receiverUsername,
                result.type(), result.textContent(),
                result.payload(), result.payloadType(),
                result.fileUrls(), result.status(), result.sentAt(),
                result.deleted(), result.editedAt(), result.expiresAt(),
                notifyUsernames
        );
    }
}
