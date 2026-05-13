package com.example.backend.module.messagemanagement.controller;

import com.example.backend.common.service.JwtService;
import com.example.backend.module.messagemanagement.application.MessageService;
import com.example.backend.module.messagemanagement.dto.SendMessageRequest;
import com.example.backend.module.messagemanagement.dto.MessageResponse;
import com.example.backend.module.messagemanagement.dto.ws.ReadReceiptEvent;
import com.example.backend.module.messagemanagement.dto.ws.TypingEvent;
import com.example.backend.module.messagemanagement.dto.ws.WsInboundMessage;
import com.example.backend.module.messagemanagement.realtime.messaging.MessageProducer;
import com.example.backend.module.messagemanagement.realtime.messaging.event.MessageEvent;
import com.example.backend.module.messagemanagement.realtime.messaging.event.PresenceRabbitEvent;
import com.example.backend.module.messagemanagement.realtime.messaging.event.ReadReceiptRabbitEvent;
import com.example.backend.module.messagemanagement.realtime.presence.PresenceService;
import com.example.backend.module.messagemanagement.realtime.session.WebSocketSessionRegistry;
import com.example.backend.module.usermanagement.persistence.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;

/**
 * Controlador STOMP — maneja frames entrantes del WebSocket.
 *
 * Destinos de suscripción del cliente (outbound):
 *   /topic/conversation.{id}   → mensajes del chat
 *   /topic/typing.{id}         → indicadores de escritura
 *   /topic/read.{id}           → confirmaciones de lectura
 *   /topic/presence.{userId}   → presencia online/offline
 *   /user/queue/notifications  → notificaciones personales
 *
 * Destinos de publicación del cliente (inbound → /app/...):
 *   /app/chat.send             → enviar mensaje
 *   /app/chat.typing           → indicar que está escribiendo
 *   /app/chat.read             → marcar mensajes como leídos
 *   /app/heartbeat             → renovar presencia
 */
@Slf4j
@Controller
public class WebSocketController {

    @Autowired private MessageService       messageService;
    @Autowired private MessageProducer      messageProducer;
    @Autowired private PresenceService      presenceService;
    @Autowired private WebSocketSessionRegistry sessionRegistry;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private UserRepository       userRepository;
    @Autowired private JwtService           jwtService;

    // ─── Conexión ─────────────────────────────────────────────────────────────

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal == null) return;

        String sessionId = accessor.getSessionId();
        Long userId = extractUserIdFromPrincipal(principal);
        if (userId == null) return;

        sessionRegistry.registerSession(userId, sessionId);
        presenceService.markOnline(userId);

        String username = principal.getName();
        PresenceRabbitEvent presenceEvent = new PresenceRabbitEvent(userId, username, true, null);
        messageProducer.publishPresence(presenceEvent);

        log.info("WS CONNECTED: user={} session={}", username, sessionId);
    }

    // ─── Desconexión ─────────────────────────────────────────────────────────

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = event.getSessionId();
        Principal principal = accessor.getUser();
        if (principal == null) return;

        Long userId = extractUserIdFromPrincipal(principal);
        if (userId == null) return;

        sessionRegistry.removeSession(userId, sessionId);

        // Solo marcar offline si no quedan otras sesiones activas
        if (!sessionRegistry.hasActiveSessions(userId)) {
            presenceService.markOffline(userId);
            String lastSeen = presenceService.getLastSeen(userId);
            String username = principal.getName();

            PresenceRabbitEvent presenceEvent =
                    new PresenceRabbitEvent(userId, username, false, lastSeen);
            messageProducer.publishPresence(presenceEvent);

            log.info("WS DISCONNECTED (all sessions): user={}", username);
        } else {
            log.info("WS DISCONNECTED (sesiones restantes): user={} session={}",
                    principal.getName(), sessionId);
        }
    }

    // ─── Enviar mensaje ──────────────────────────────────────────────────────

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload WsInboundMessage inbound,
                            SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) return;

        Long senderId = extractUserIdFromPrincipal(principal);
        if (senderId == null) return;

        // Persistir en BD vía servicio existente
        SendMessageRequest req = new SendMessageRequest(
                inbound.conversationId(),
                inbound.type(),
                inbound.textContent(),
                inbound.payload(),
                inbound.payloadType(),
                inbound.fileUrls()
        );

        MessageResponse saved = messageService.sendMessage(senderId, req);

        // Publicar en RabbitMQ para procesamiento asíncrono y broadcast
        MessageEvent event = new MessageEvent(
                saved.messageId(),
                saved.conversationId(),
                saved.senderId(),
                principal.getName(),
                saved.receiverId(),
                saved.type(),
                saved.textContent(),
                saved.payload(),
                saved.payloadType(),
                saved.fileUrls(),
                saved.status(),
                saved.sentAt()
        );
        messageProducer.publishMessage(event);
    }

    // ─── Typing indicator ─────────────────────────────────────────────────────

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingEvent event,
                       SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) return;

        Long userId = extractUserIdFromPrincipal(principal);
        if (userId == null) return;

        if (event.typing()) {
            presenceService.setTyping(event.conversationId(), userId);
        } else {
            presenceService.clearTyping(event.conversationId(), userId);
        }

        // Broadcast directo (latencia mínima, sin pasar por RabbitMQ)
        TypingEvent broadcast = new TypingEvent(
                event.conversationId(),
                userId,
                principal.getName(),
                event.typing()
        );
        messagingTemplate.convertAndSend(
                "/topic/typing." + event.conversationId(),
                broadcast
        );
    }

    // ─── Marcar como leído ────────────────────────────────────────────────────

    @MessageMapping("/chat.read")
    public void markRead(@Payload ReadReceiptEvent event,
                         SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) return;

        Long userId = extractUserIdFromPrincipal(principal);
        if (userId == null) return;

        int updated = messageService.markAsRead(event.conversationId(), userId);
        if (updated == 0) return;

        ReadReceiptRabbitEvent rabbitEvent = new ReadReceiptRabbitEvent(
                event.conversationId(),
                userId,
                principal.getName(),
                updated,
                LocalDateTime.now()
        );
        messageProducer.publishReadReceipt(rabbitEvent);
    }

    // ─── Heartbeat ────────────────────────────────────────────────────────────

    @MessageMapping("/heartbeat")
    public void heartbeat(SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) return;

        Long userId = extractUserIdFromPrincipal(principal);
        if (userId != null) {
            presenceService.heartbeat(userId);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Long extractUserIdFromPrincipal(Principal principal) {
        if (principal instanceof org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken auth) {
            Object details = auth.getDetails();
            if (details instanceof Long l) return l;
            if (details instanceof Number n) return n.longValue();
        }
        return null;
    }
}