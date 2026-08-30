package com.example.backend.module.messagemanagement.controller;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;
import com.example.backend.module.messagemanagement.dto.PresenceSnapshotResponse;
import com.example.backend.module.messagemanagement.realtime.presence.IPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /api/presence?ids=1,2,3 → estado actual de presencia (online/lastSeen)
 * de cada id pedido.
 *
 * Complementa /topic/presence.{userId} (WS): ese topic solo avisa CAMBIOS
 * de estado a quien ya esté suscripto en el momento en que ocurren — no hay
 * "replay". Sin este endpoint, un cliente que abre la app después de que el
 * otro ya se conectó nunca se entera de que está online hasta la próxima
 * vez que esa persona se conecte o desconecte. El frontend llama a esto una
 * vez al armar las suscripciones de presencia, para arrancar con el estado
 * real en vez de esperar el próximo evento.
 */
@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final IPresenceService presenceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PresenceSnapshotResponse>>> snapshot(@RequestParam List<Long> ids) {
        List<PresenceSnapshotResponse> result = ids.stream()
                .distinct()
                .map(id -> new PresenceSnapshotResponse(id, presenceService.isOnline(id), presenceService.getLastSeen(id)))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GENERIC, result));
    }
}
