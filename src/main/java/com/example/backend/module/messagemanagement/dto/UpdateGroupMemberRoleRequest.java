package com.example.backend.module.messagemanagement.dto;

import com.example.backend.common.enums.GroupRole;
import jakarta.validation.constraints.NotNull;

/** Solo admite ADMIN o MEMBER — OWNER se asigna únicamente vía transferencia de propiedad. */
public record UpdateGroupMemberRoleRequest(

        @NotNull(message = "El rol es obligatorio")
        GroupRole role

) {}
