package com.example.backend.module.messagemanagement.dto;

import com.example.backend.common.enums.MessageType;
import com.example.backend.common.enums.PayloadType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SendMessageRequest(

        @NotNull(message = "El ID de la conversación es obligatorio")
        @Positive
        Long conversationId,

        @NotNull(message = "El tipo de mensaje es obligatorio")
        MessageType type,

        /** Texto del mensaje. Requerido para type = TEXT */
        String textContent,

        /**
         * Payload estructurado. Requerido para type = PAYLOAD.
         * Se guarda como JSON en BD.
         */
        Object payload,

        /** Clasificación del payload */
        PayloadType payloadType,

        /**
         * URLs de archivos ya subidos al storage (máx. 3).
         * El cliente sube los archivos primero y envía las URLs aquí.
         */
        @Size(max = 3, message = "Se permiten máximo 3 archivos por mensaje")
        List<String> fileUrls
) {}
