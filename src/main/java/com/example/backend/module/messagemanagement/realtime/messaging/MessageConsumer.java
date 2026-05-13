package com.example.backend.module.messagemanagement.realtime.messaging;

import com.example.backend.module.messagemanagement.dto.ws.PresenceEvent;
import com.example.backend.module.messagemanagement.dto.ws.ReadReceiptEvent;
import com.example.backend.module.messagemanagement.dto.ws.WsOutboundMessage;
import com.example.backend.module.messagemanagement.realtime.config.RabbitMqConfig;
import com.example.backend.module.messagemanagement.realtime.messaging.event.MessageEvent;
import com.example.backend.module.messagemanagement.realtime.messaging.event.PresenceRabbitEvent;
import com.example.backend.module.messagemanagement.realtime.messaging.event.ReadReceiptRabbitEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Consumidor de colas RabbitMQ.
 *
 * Cada mensaje consumido de RabbitMQ se hace broadcast a los clientes
 * WebSocket correspondientes mediante STOMP.
 *
 * Tópicos STOMP de salida:
 *   /topic/conversation.{conversationId}   → mensaje nuevo
 *   /topic/read.{conversationId}           → confirmación de lectura
 *   /topic/presence.{userId}               → online/offline
 */
@Slf4j
@Component
public class MessageConsumer {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // ─── Consumer: mensajes ──────────────────────────────────────────────────

    @RabbitListener(queues = RabbitMqConfig.QUEUE_MESSAGES,
            containerFactory = "rabbitListenerContainerFactory")
    public void consumeMessage(MessageEvent event) {
        log.debug("Consumiendo MESSAGE: msgId={} convId={}", event.messageId(), event.conversationId());

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

        // Broadcast a todos los suscriptores de la conversación
        messagingTemplate.convertAndSend(
                "/topic/conversation." + event.conversationId(),
                outbound
        );

        // Notificación personal al receptor (incluso si no está en esa pantalla)
        messagingTemplate.convertAndSendToUser(
                event.receiverId().toString(),
                "/queue/notifications",
                outbound
        );
    }

    // ─── Consumer: read receipts ─────────────────────────────────────────────

    @RabbitListener(queues = RabbitMqConfig.QUEUE_READ_RECEIPTS,
            containerFactory = "rabbitListenerContainerFactory")
    public void consumeReadReceipt(ReadReceiptRabbitEvent event) {
        log.debug("Consumiendo READ_RECEIPT: convId={} userId={}",
                event.conversationId(), event.readByUserId());

        ReadReceiptEvent wsEvent = new ReadReceiptEvent(
                event.conversationId(),
                event.readByUserId(),
                event.readByUsername(),
                event.readAt() != null ? event.readAt() : LocalDateTime.now()
        );

        messagingTemplate.convertAndSend(
                "/topic/read." + event.conversationId(),
                wsEvent
        );
    }

    // ─── Consumer: presencia ─────────────────────────────────────────────────

    @RabbitListener(queues = RabbitMqConfig.QUEUE_PRESENCE,
            containerFactory = "rabbitListenerContainerFactory")
    public void consumePresence(PresenceRabbitEvent event) {
        log.debug("Consumiendo PRESENCE: userId={} online={}", event.userId(), event.online());

        PresenceEvent wsEvent = new PresenceEvent(
                event.userId(),
                event.username(),
                event.online(),
                event.lastSeen()
        );

        messagingTemplate.convertAndSend(
                "/topic/presence." + event.userId(),
                wsEvent
        );
    }
}