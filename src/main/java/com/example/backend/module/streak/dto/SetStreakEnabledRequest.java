package com.example.backend.module.streak.dto;

/** true → dispara solicitud de activación (opt-in mutuo). false → desactiva de inmediato. */
public record SetStreakEnabledRequest(boolean enabled) {}
