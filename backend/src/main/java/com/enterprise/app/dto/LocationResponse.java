package com.enterprise.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Location details")
public record LocationResponse(

    @Schema(description = "Location ID")
    UUID id,

    @Schema(description = "Organization this location belongs to")
    UUID organizationId,

    @Schema(description = "Location name")
    String name,

    @Schema(description = "Street address")
    String address,

    @Schema(description = "City")
    String city,

    @Schema(description = "State code")
    String stateCode,

    @Schema(description = "ZIP code")
    String zip,

    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt,

    @Schema(description = "Last update timestamp")
    LocalDateTime updatedAt
) {}
