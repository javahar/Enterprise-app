package com.enterprise.app.controller;

import com.enterprise.app.dto.LocationRequest;
import com.enterprise.app.dto.LocationResponse;
import com.enterprise.app.dto.PageResponse;
import com.enterprise.app.service.LocationService;
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
@Tag(name = "Locations", description = "Location management APIs")
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/api/organizations/{orgId}/locations")
    @Operation(summary = "List all locations for an organization (paginated)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page returned successfully"),
        @ApiResponse(responseCode = "404", description = "Organization not found or inactive")
    })
    public ResponseEntity<PageResponse<LocationResponse>> getAllByOrg(
            @Parameter(description = "Organization ID") @PathVariable UUID orgId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(locationService.getAllByOrg(orgId, page, size));
    }

    @PostMapping("/api/organizations/{orgId}/locations")
    @Operation(summary = "Create a location under an organization")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Location created"),
        @ApiResponse(responseCode = "400", description = "Validation error or duplicate name in org"),
        @ApiResponse(responseCode = "404", description = "Organization not found or inactive")
    })
    public ResponseEntity<LocationResponse> create(
            @Parameter(description = "Organization ID") @PathVariable UUID orgId,
            @Valid @RequestBody LocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locationService.create(orgId, request));
    }

    @GetMapping("/api/locations/{id}")
    @Operation(summary = "Get a location by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Location found"),
        @ApiResponse(responseCode = "404", description = "Location not found")
    })
    public ResponseEntity<LocationResponse> getById(
            @Parameter(description = "Location ID") @PathVariable UUID id) {
        return ResponseEntity.ok(locationService.getById(id));
    }

    @PutMapping("/api/locations/{id}")
    @Operation(summary = "Update a location")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Location updated"),
        @ApiResponse(responseCode = "400", description = "Validation error or duplicate name in org"),
        @ApiResponse(responseCode = "404", description = "Location not found")
    })
    public ResponseEntity<LocationResponse> update(
            @Parameter(description = "Location ID") @PathVariable UUID id,
            @Valid @RequestBody LocationRequest request) {
        return ResponseEntity.ok(locationService.update(id, request));
    }

    @DeleteMapping("/api/locations/{id}")
    @Operation(summary = "Hard-delete a location")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Location deleted"),
        @ApiResponse(responseCode = "404", description = "Location not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Location ID") @PathVariable UUID id) {
        locationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
