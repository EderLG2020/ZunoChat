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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true")
public class MessageConsumer {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMqConfig.QUEUE_MESSAGES,
            containerFactory = "rabbitListenerContainerFactory")
    public void consumeMessage(MessageEvent event) {
        log.debug("Consumiendo MESSAGE: msgId={} convId={}", event.messageId(), event.conversationId());
        WsOutboundMessage outbound = new WsOutboundMessage(
                "MESSAGE_RECEIVED", event.messageId(), event.conversationId(),
                event.senderId(), event.senderUsername(), event.receiverId(),
                event.type(), event.textContent(), event.payload(), event.payloadType(),
                event.fileUrls(), event.status(), event.sentAt()
        );

        messagingTemplate.convertAndSend("/topic/conversation." + event.conversationId(), outbound);

        messagingTemplate.convertAndSendToUser(
                event.receiverUsername(), "/queue/notifications", outbound);
        messagingTemplate.convertAndSendToUser(
                event.senderUsername(), "/queue/notifications", outbound);
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_READ_RECEIPTS,
            containerFactory = "rabbitListenerContainerFactory")
    public void consumeReadReceipt(ReadReceiptRabbitEvent event) {
        log.debug("Consumiendo READ_RECEIPT: convId={} userId={}", event.conversationId(), event.readByUserId());
        ReadReceiptEvent wsEvent = new ReadReceiptEvent(
                event.conversationId(), event.readByUserId(), event.readByUsername(),
                event.readAt() != null ? event.readAt() : LocalDateTime.now()
        );
        messagingTemplate.convertAndSend("/topic/read." + event.conversationId(), wsEvent);
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_PRESENCE,
            containerFactory = "rabbitListenerContainerFactory")
    public void consumePresence(PresenceRabbitEvent event) {
        log.debug("Consumiendo PRESENCE: userId={} online={}", event.userId(), event.online());
        PresenceEvent wsEvent = new PresenceEvent(
                event.userId(), event.username(), event.online(), event.lastSeen());
        messagingTemplate.convertAndSend("/topic/presence." + event.userId(), wsEvent);
    }
}