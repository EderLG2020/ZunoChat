package com.example.backend.module.messagemanagement.realtime.messaging;

import com.example.backend.module.messagemanagement.realtime.messaging.event.MessageEvent;
import com.example.backend.module.messagemanagement.realtime.messaging.event.PresenceBroadcastEvent;
import com.example.backend.module.messagemanagement.realtime.messaging.event.ReadReceiptBroadcastEvent;

public interface IMessageProducer {
    void publishMessage(MessageEvent event);
    /** Igual payload que publishMessage, pero eventType="MESSAGE_UPDATED" — usado al editar/borrar un mensaje. */
    void publishMessageUpdate(MessageEvent event);
    void publishReadReceipt(ReadReceiptBroadcastEvent event);
    void publishPresence(PresenceBroadcastEvent event);
}