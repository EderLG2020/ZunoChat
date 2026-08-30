package com.example.backend.module.streak.controller;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;
import com.example.backend.common.util.JwtUtil;
import com.example.backend.module.streak.application.StreakService;
import com.example.backend.module.streak.dto.RespondStreakRequest;
import com.example.backend.module.streak.dto.SetStreakEnabledRequest;
import com.example.backend.module.streak.dto.StreakResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * GET   /api/streaks/{conversationId}          → estado actual de la racha
 * PATCH /api/streaks/{conversationId}          → activar (envía solicitud) o desactivar (inmediato)
 * POST  /api/streaks/{conversationId}/respond  → aceptar/rechazar una solicitud pendiente
 */
@RestController
@RequestMapping("/api/streaks")
@Validated
public class StreakController {

    @Autowired private StreakService streakService;

    @GetMapping("/{conversationId}")
    public ResponseEntity<ApiResponse<StreakResponse>> get(@PathVariable Long conversationId) {
        Long userId = JwtUtil.currentUserId();
        StreakResponse result = streakService.getStreak(userId, conversationId);
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GENERIC, result));
    }

    /** enabled=true dispara/acepta una solicitud de activación; enabled=false desactiva de inmediato. */
    @PatchMapping("/{conversationId}")
    public ResponseEntity<ApiResponse<StreakResponse>> setEnabled(
            @PathVariable Long conversationId, @RequestBody SetStreakEnabledRequest req) {
        Long userId = JwtUtil.currentUserId();
        StreakResponse result = req.enabled()
                ? streakService.requestActivation(userId, conversationId)
                : streakService.disable(userId, conversationId);
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GENERIC, result));
    }

    @PostMapping("/{conversationId}/respond")
    public ResponseEntity<ApiResponse<StreakResponse>> respond(
            @PathVariable Long conversationId, @RequestBody RespondStreakRequest req) {
        Long userId = JwtUtil.currentUserId();
        StreakResponse result = streakService.respondToActivation(userId, conversationId, req.accept());
        return ResponseEntity.ok(ApiResponse.ok(AppCode.OK_GENERIC, result));
    }
}
