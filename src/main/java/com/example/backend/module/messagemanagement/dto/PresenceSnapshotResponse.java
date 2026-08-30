package com.example.backend.module.messagemanagement.dto;

/**
 * Estado de presencia actual de un usuario, para "sembrar" el store de
 * presencia del frontend al cargar (ver PresenceController). El WS
 * (/topic/presence.{userId}) solo empuja CAMBIOS de estado a quien ya esté
 * suscripto en ese momento — si alguien se conectó antes de que el otro
 * abriera la app, ese evento ya pasó y nunca llega. Este snapshot cubre
 * ese hueco: da el estado actual sin depender de haber estado escuchando
 * en el momento exacto de la transición.
 */
public record PresenceSnapshotResponse(
        Long userId,
        boolean online,
        String lastSeen
) {}
