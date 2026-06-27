package com.enterprise.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Organization details")
public record OrganizationResponse(

    @Schema(description = "Organization ID")
    UUID id,

    @Schema(description = "Organization name")
    String name,

    @Schema(description = "Organization description")
    String description,

    @Schema(description = "Whether the organization is active")
    boolean isActive,

    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt,

    @Schema(description = "Last update timestamp")
    LocalDateTime updatedAt
) {}