package com.example.backend.module.messagemanagement.dto;

import jakarta.validation.constraints.NotNull;

public record TransferGroupOwnershipRequest(

        @NotNull(message = "Selecciona el nuevo propietario")
        Long newOwnerUserId

) {}
