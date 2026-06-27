package com.enterprise.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for creating or updating an organization")
public record OrganizationRequest(

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Schema(description = "Organization name", example = "Acme Corp", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,

    @Schema(description = "Optional description", example = "Our main operating entity")
    String description,

    @Schema(description = "Active status; use false on update to deactivate. Ignored on create (always true).", example = "true")
    Boolean isActive
) {}