package com.enterprise.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for creating or updating a location")
public record LocationRequest(

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Schema(description = "Location name", example = "Downtown Office", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,

    @Schema(description = "Street address", example = "123 Main St")
    String address,

    @Schema(description = "City", example = "Springfield")
    String city,

    @Size(max = 50, message = "State code must not exceed 50 characters")
    @Schema(description = "State code", example = "IL")
    String stateCode,

    @Schema(description = "ZIP code", example = "62701")
    String zip
) {}
