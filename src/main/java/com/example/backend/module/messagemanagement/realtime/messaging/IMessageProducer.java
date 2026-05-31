package com.example.backend.module.messagemanagement.realtime.messaging;

import com.example.backend.module.messagemanagement.realtime.messaging.event.MessageEvent;
import com.example.backend.module.messagemanagement.realtime.messaging.event.PresenceRabbitEvent;
import com.example.backend.module.messagemanagement.realtime.messaging.event.ReadReceiptRabbitEvent;

public interface IMessageProducer {
    void publishMessage(MessageEvent event);
    void publishReadReceipt(ReadReceiptRabbitEvent event);
    void publishPresence(PresenceRabbitEvent event);
}