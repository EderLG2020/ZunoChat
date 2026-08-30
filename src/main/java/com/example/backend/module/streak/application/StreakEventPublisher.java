package com.example.backend.module.streak.application;

import com.example.backend.module.streak.domain.StreakModel;
import com.example.backend.module.streak.dto.ws.StreakEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Hace el broadcast WebSocket de cambios de racha — mismo patrón que
 * MessageProducer: topic dedicado por conversación (no reutiliza
 * /topic/conversation.{id}, igual que /topic/typing.{id} o /topic/read.{id})
 * y corre en wsBroadcastExecutor para no atar la transacción que disparó el
 * cambio al fan-out del broker.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreakEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Async("wsBroadcastExecutor")
    public void publish(String eventType, StreakModel streak) {
        log.debug("Streak broadcast {}: convId={} count={} status={}",
                eventType, streak.getConversationId(), streak.getCurrentCount(), streak.getStatus());

        StreakEvent event = new StreakEvent(
                eventType,
                streak.getConversationId(),
                streak.getCurrentCount(),
                streak.getLongestCount(),
                streak.getStatus(),
                streak.getRequestedByUserId()
        );

        messagingTemplate.convertAndSend("/topic/streak." + streak.getConversationId(), event);
    }
}
