package com.example.backend.module.messagemanagement.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddGroupMembersRequest(

        @NotEmpty(message = "Selecciona al menos un usuario para agregar")
        List<Long> memberIds

) {}
