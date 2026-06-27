package com.enterprise.app.controller;

import com.enterprise.app.dto.LocationUserCountResponse;
import com.enterprise.app.dto.PageResponse;
import com.enterprise.app.dto.UserLocationRequest;
import com.enterprise.app.dto.UserLocationResponse;
import com.enterprise.app.service.UserLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "User Locations", description = "User-location assignment APIs")
public class UserLocationController {

    private final UserLocationService userLocationService;

    @GetMapping("/api/users/{userId}/locations")
    @Operation(summary = "List all location assignments for a user (paginated)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page returned"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<PageResponse<UserLocationResponse>> getLocationsForUser(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userLocationService.getLocationsForUser(userId, page, size));
    }

    @PostMapping("/api/users/{userId}/locations")
    @Operation(summary = "Assign a user to a location with a role (upsert — updates role if already assigned)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Assignment created or updated"),
        @ApiResponse(responseCode = "400", description = "Validation error or user/location in different orgs"),
        @ApiResponse(responseCode = "404", description = "User or location not found")
    })
    public ResponseEntity<UserLocationResponse> assign(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Valid @RequestBody UserLocationRequest request) {
        return ResponseEntity.ok(userLocationService.assign(userId, request));
    }

    @DeleteMapping("/api/users/{userId}/locations/{locationId}")
    @Operation(summary = "Remove a user-location assignment")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Assignment removed"),
        @ApiResponse(responseCode = "404", description = "Assignment not found")
    })
    public ResponseEntity<Void> removeAssignment(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Location ID") @PathVariable UUID locationId) {
        userLocationService.removeAssignment(userId, locationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/locations/{locationId}/users/count")
    @Operation(summary = "Count users assigned to a location (use before deleting a location)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Count returned"),
        @ApiResponse(responseCode = "404", description = "Location not found")
    })
    public ResponseEntity<LocationUserCountResponse> countUsersAtLocation(
            @Parameter(description = "Location ID") @PathVariable UUID locationId) {
        return ResponseEntity.ok(userLocationService.countUsersAtLocation(locationId));
    }
}
