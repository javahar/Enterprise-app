package com.enterprise.app.dto;

import com.enterprise.app.entity.AccessRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request body for assigning a user to a location")
public record UserLocationRequest(

    @NotNull(message = "Location ID is required")
    @Schema(description = "Location to assign the user to", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID locationId,

    @NotNull(message = "Role is required")
    @Schema(description = "Role for this assignment", example = "READ", allowableValues = {"READ", "WRITE", "ADMIN"}, requiredMode = Schema.RequiredMode.REQUIRED)
    AccessRole role
) {}
