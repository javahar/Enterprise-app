package com.enterprise.app.controller;

import com.enterprise.app.dto.AppUserRequest;
import com.enterprise.app.dto.AppUserResponse;
import com.enterprise.app.dto.PageResponse;
import com.enterprise.app.service.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class AppUserController {

    private final AppUserService appUserService;

    @GetMapping("/api/organizations/{orgId}/users")
    @Operation(summary = "List all users for an organization, paginated (active and inactive)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page returned successfully"),
        @ApiResponse(responseCode = "404", description = "Organization not found or inactive")
    })
    public ResponseEntity<PageResponse<AppUserResponse>> getAllByOrg(
            @Parameter(description = "Organization ID") @PathVariable UUID orgId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(appUserService.getAllByOrg(orgId, page, size));
    }

    @PostMapping("/api/organizations/{orgId}/users")
    @Operation(summary = "Create a user under an organization")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created"),
        @ApiResponse(responseCode = "400", description = "Validation error or duplicate email"),
        @ApiResponse(responseCode = "404", description = "Organization not found or inactive")
    })
    public ResponseEntity<AppUserResponse> create(
            @Parameter(description = "Organization ID") @PathVariable UUID orgId,
            @Valid @RequestBody AppUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appUserService.create(orgId, request));
    }

    @GetMapping("/api/users/{id}")
    @Operation(summary = "Get a user by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<AppUserResponse> getById(
            @Parameter(description = "User ID") @PathVariable UUID id) {
        return ResponseEntity.ok(appUserService.getById(id));
    }

    @PutMapping("/api/users/{id}")
    @Operation(summary = "Update a user (name, email, or isActive status)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated"),
        @ApiResponse(responseCode = "400", description = "Validation error or duplicate email"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<AppUserResponse> update(
            @Parameter(description = "User ID") @PathVariable UUID id,
            @Valid @RequestBody AppUserRequest request) {
        return ResponseEntity.ok(appUserService.update(id, request));
    }

    @DeleteMapping("/api/users/{id}")
    @Operation(summary = "Hard-delete a user")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "User ID") @PathVariable UUID id) {
        appUserService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
