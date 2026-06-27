package com.enterprise.app.dto;

import com.enterprise.app.entity.AccessRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "User-location assignment details")
public record UserLocationResponse(

    @Schema(description = "User ID")
    UUID userId,

    @Schema(description = "Location ID")
    UUID locationId,

    @Schema(description = "Location name")
    String locationName,

    @Schema(description = "City")
    String city,

    @Schema(description = "State code")
    String stateCode,

    @Schema(description = "Role assigned at this location")
    AccessRole role,

    @Schema(description = "When the assignment was created or last updated")
    LocalDateTime assignedAt
) {}
