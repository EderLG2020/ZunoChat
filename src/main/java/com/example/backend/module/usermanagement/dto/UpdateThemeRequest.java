package com.example.backend.module.usermanagement.dto;

import com.example.backend.common.enums.ThemePreference;
import jakarta.validation.constraints.NotNull;

public record UpdateThemeRequest(

        @NotNull(message = "El tema es obligatorio")
        ThemePreference theme

) {}
