package com.example.backend.module.messagemanagement.application;

import com.example.backend.module.messagemanagement.domain.MessageModel;
import com.example.backend.module.messagemanagement.dto.MessageResponse;
import com.example.backend.module.messagemanagement.persistence.MessageRepository;
import com.example.backend.module.messagemanagement.realtime.messaging.IMessageProducer;
import com.example.backend.module.messagemanagement.realtime.messaging.MessageEventFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Autoelimina (soft-delete) los mensajes de "chat temporal" cuya expiresAt ya
 * pasó — ver ConversationModel#ephemeralEnabled y MessageService#sendMessage.
 *
 * Reusa el mismo soft-delete que un borrado manual (limpia textContent/payload/
 * fileUrls, marca deleted=true) y emite MESSAGE_UPDATED por WS para que
 * cualquier chat abierto lo reemplace por el tombstone al instante, en vez de
 * esperar a que alguien recargue.
 *
 * Corre cada minuto; procesa como mucho BATCH_SIZE por tick para no acaparar
 * la conexión a BD si en algún momento se acumulan muchos vencidos de golpe
 * (el próximo tick retoma el resto).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EphemeralMessageSweeper {

    private static final int BATCH_SIZE = 200;

    private final MessageRepository    messageRepository;
    private final MessageService       messageService;
    private final MessageEventFactory  messageEventFactory;
    private final IMessageProducer     messageProducer;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void sweepExpiredMessages() {
        List<MessageModel> expired = messageRepository.findExpiredNotDeleted(
                LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE));

        if (expired.isEmpty()) return;

        for (MessageModel msg : expired) {
            msg.setDeleted(true);
            msg.setTextContent(null);
            msg.setPayload(null);
            msg.setFileUrls(null);
            MessageModel saved = messageRepository.save(msg);

            MessageResponse response = messageService.toResponse(saved);
            // actingUserId = el propio emisor: no hay "quién lo borró" real (fue el
            // sistema), y el emisor es el único dato que ya tenemos sin otra consulta.
            messageProducer.publishMessageUpdate(
                    messageEventFactory.from(response, saved.getSenderId(), null));
        }

        log.info("[EphemeralMessageSweeper] {} mensaje(s) de chat temporal autoeliminados", expired.size());
    }
}
