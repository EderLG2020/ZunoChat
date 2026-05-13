package com.example.backend.module.messagemanagement.realtime.messaging;

import com.example.backend.module.messagemanagement.realtime.config.RabbitMqConfig;
import com.example.backend.module.messagemanagement.realtime.messaging.event.MessageEvent;
import com.example.backend.module.messagemanagement.realtime.messaging.event.PresenceRabbitEvent;
import com.example.backend.module.messagemanagement.realtime.messaging.event.ReadReceiptRabbitEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Productor de eventos RabbitMQ.
 * Publicación asíncrona — el caller no espera confirmación de procesamiento.
 */
@Slf4j
@Component
public class MessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void publishMessage(MessageEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE_DIRECT,
                    RabbitMqConfig.RK_MESSAGE,
                    event
            );
            log.debug("Publicado MESSAGE event: msgId={} convId={}",
                    event.messageId(), event.conversationId());
        } catch (AmqpException e) {
            log.error("Error publicando MESSAGE event: {}", e.getMessage(), e);
        }
    }

    public void publishReadReceipt(ReadReceiptRabbitEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE_DIRECT,
                    RabbitMqConfig.RK_READ,
                    event
            );
            log.debug("Publicado READ_RECEIPT event: convId={} userId={}",
                    event.conversationId(), event.readByUserId());
        } catch (AmqpException e) {
            log.error("Error publicando READ_RECEIPT event: {}", e.getMessage(), e);
        }
    }

    public void publishPresence(PresenceRabbitEvent event) {
        try {
            // Fanout → se enruta automáticamente a todos los queues enlazados
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE_FANOUT,
                    "",   // routing key ignorada en fanout
                    event
            );
            log.debug("Publicado PRESENCE event: userId={} online={}",
                    event.userId(), event.online());
        } catch (AmqpException e) {
            log.error("Error publicando PRESENCE event: {}", e.getMessage(), e);
        }
    }
}