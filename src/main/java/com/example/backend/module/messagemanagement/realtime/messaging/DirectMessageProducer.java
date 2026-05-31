package com.example.backend.module.messagemanagement.realtime.messaging;

import com.example.backend.module.messagemanagement.dto.ws.PresenceEvent;
import com.example.backend.module.messagemanagement.dto.ws.ReadReceiptEvent;
import com.example.backend.module.messagemanagement.dto.ws.WsOutboundMessage;
import com.example.backend.module.messagemanagement.realtime.messaging.event.MessageEvent;
import com.example.backend.module.messagemanagement.realtime.messaging.event.PresenceRabbitEvent;
import com.example.backend.module.messagemanagement.realtime.messaging.event.ReadReceiptRabbitEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Productor directo (sin RabbitMQ).
 * Cuando rabbitmq.enabled=false, hace el broadcast WebSocket de forma síncrona
 * en el mismo hilo, sin pasar por ningún broker externo.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "false", matchIfMissing = true)
public class DirectMessageProducer implements IMessageProducer {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void publishMessage(MessageEvent event) {
        log.debug("Direct broadcast MESSAGE: msgId={} convId={}", event.messageId(), event.conversationId());

        WsOutboundMessage outbound = new WsOutboundMessage(
                "MESSAGE_RECEIVED",
                event.messageId(),
                event.conversationId(),
                event.senderId(),
                event.senderUsername(),
                event.receiverId(),
                event.type(),
                event.textContent(),
                event.payload(),
                event.payloadType(),
                event.fileUrls(),
                event.status(),
                event.sentAt()
        );

        // Broadcast al topic de la conversación (ambos usuarios suscritos lo reciben)
        messagingTemplate.convertAndSend("/topic/conversation." + event.conversationId(), outbound);

        // ✅ Notificación al receptor (por username, que es lo que usa el Principal)
        messagingTemplate.convertAndSendToUser(
                event.receiverUsername(), "/queue/notifications", outbound);

        // ✅ Notificación al emisor para que su sidebar actualice el último mensaje
        messagingTemplate.convertAndSendToUser(
                event.senderUsername(), "/queue/notifications", outbound);
    }

    public void publishReadReceipt(ReadReceiptRabbitEvent event) {
        log.debug("Direct broadcast READ_RECEIPT: convId={} userId={}", event.conversationId(), event.readByUserId());

        ReadReceiptEvent wsEvent = new ReadReceiptEvent(
                event.conversationId(),
                event.readByUserId(),
                event.readByUsername(),
                event.readAt() != null ? event.readAt() : LocalDateTime.now()
        );

        messagingTemplate.convertAndSend("/topic/read." + event.conversationId(), wsEvent);
    }

    public void publishPresence(PresenceRabbitEvent event) {
        log.debug("Direct broadcast PRESENCE: userId={} online={}", event.userId(), event.online());

        PresenceEvent wsEvent = new PresenceEvent(
                event.userId(),
                event.username(),
                event.online(),
                event.lastSeen()
        );

        messagingTemplate.convertAndSend("/topic/presence." + event.userId(), wsEvent);
    }
}