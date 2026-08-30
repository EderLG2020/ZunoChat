package com.example.backend.module.messagemanagement.realtime.messaging;

import com.example.backend.module.messagemanagement.dto.ws.PresenceEvent;
import com.example.backend.module.messagemanagement.dto.ws.ReadReceiptEvent;
import com.example.backend.module.messagemanagement.dto.ws.WsOutboundMessage;
import com.example.backend.module.messagemanagement.realtime.messaging.event.MessageEvent;
import com.example.backend.module.messagemanagement.realtime.messaging.event.PresenceBroadcastEvent;
import com.example.backend.module.messagemanagement.realtime.messaging.event.ReadReceiptBroadcastEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Hace el broadcast WebSocket — con el broker simple (default) es en
 * memoria del mismo JVM; con app.websocket.relay.enabled=true, cada
 * convertAndSend depende de I/O de red hacia RabbitMQ. Todos los métodos
 * corren en wsBroadcastExecutor (@Async) para no atar la respuesta HTTP al
 * fan-out del broker — el caller (controller) ya construyó el evento con
 * todos los datos que necesita, así que no hay estado de request/hilo
 * (SecurityContext, transacción) del que este código dependa.
 */
@Slf4j
@Component
public class MessageProducer implements IMessageProducer {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Async("wsBroadcastExecutor")
    public void publishMessage(MessageEvent event) {
        log.debug("Direct broadcast MESSAGE: msgId={} convId={}", event.messageId(), event.conversationId());

        WsOutboundMessage outbound = toOutbound("MESSAGE_RECEIVED", event);

        // Broadcast al topic de la conversación (todos los suscritos lo reciben —
        // en GROUP, cualquier cantidad de miembros con el chat abierto)
        messagingTemplate.convertAndSend("/topic/conversation." + event.conversationId(), outbound);

        // ✅ Notificación a cada participante para que actualice su sidebar
        // (preview, orden, no-leídos) — [sender, receiver] en DIRECT, todos
        // los miembros en GROUP (ver MessageEventFactory).
        for (String username : event.notifyUsernames()) {
            messagingTemplate.convertAndSendToUser(username, "/queue/notifications", outbound);
        }
    }

    @Async("wsBroadcastExecutor")
    public void publishMessageUpdate(MessageEvent event) {
        log.debug("Direct broadcast MESSAGE_UPDATED: msgId={} convId={} deleted={}",
                event.messageId(), event.conversationId(), event.deleted());

        WsOutboundMessage outbound = toOutbound("MESSAGE_UPDATED", event);

        // Solo al topic de la conversación — ambos lados actualizan el mensaje
        // en su lista local buscándolo por messageId. No toca el sidebar
        // (el preview de la conversación puede quedar desactualizado si el
        // mensaje editado/borrado era el último; limitación aceptada).
        messagingTemplate.convertAndSend("/topic/conversation." + event.conversationId(), outbound);
    }

    private WsOutboundMessage toOutbound(String eventType, MessageEvent event) {
        return new WsOutboundMessage(
                eventType,
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
                event.sentAt(),
                event.deleted(),
                event.editedAt(),
                event.expiresAt()
        );
    }

    @Async("wsBroadcastExecutor")
    public void publishReadReceipt(ReadReceiptBroadcastEvent event) {
        log.debug("Direct broadcast READ_RECEIPT: convId={} userId={}", event.conversationId(), event.readByUserId());

        ReadReceiptEvent wsEvent = new ReadReceiptEvent(
                event.conversationId(),
                event.readByUserId(),
                event.readByUsername(),
                event.readAt() != null ? event.readAt() : LocalDateTime.now()
        );

        messagingTemplate.convertAndSend("/topic/read." + event.conversationId(), wsEvent);
    }

    @Async("wsBroadcastExecutor")
    public void publishPresence(PresenceBroadcastEvent event) {
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