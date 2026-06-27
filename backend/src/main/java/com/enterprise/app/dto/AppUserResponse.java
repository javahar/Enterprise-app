package com.enterprise.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "User details")
public record AppUserResponse(

    @Schema(description = "User ID")
    UUID id,

    @Schema(description = "Organization this user belongs to")
    UUID organizationId,

    @Schema(description = "First name")
    String firstName,

    @Schema(description = "Last name")
    String lastName,

    @Schema(description = "Email address")
    String email,

    @Schema(description = "Whether the user is active")
    boolean isActive,

    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt,

    @Schema(description = "Last update timestamp")
    LocalDateTime updatedAt
) {}
