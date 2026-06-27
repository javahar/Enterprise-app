package com.enterprise.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for creating or updating a user")
public record AppUserRequest(

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Schema(description = "First name", example = "Jane", requiredMode = Schema.RequiredMode.REQUIRED)
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Schema(description = "Last name", example = "Smith", requiredMode = Schema.RequiredMode.REQUIRED)
    String lastName,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(description = "Email address (globally unique)", example = "jane.smith@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    String email,

    @Schema(description = "Active status. Ignored on create (always true). Use false on update to deactivate.", example = "true")
    Boolean isActive
) {}
