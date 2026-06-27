package com.enterprise.app.controller;

import com.enterprise.app.dto.OrganizationRequest;
import com.enterprise.app.dto.OrganizationResponse;
import com.enterprise.app.dto.PageResponse;
import com.enterprise.app.service.OrganizationService;
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
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Organization management APIs")
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @Operation(summary = "List all active organizations (paginated)")
    @ApiResponse(responseCode = "200", description = "Page returned successfully")
    public ResponseEntity<PageResponse<OrganizationResponse>> getAll(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(organizationService.getAll(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an organization by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Organization found"),
        @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<OrganizationResponse> getById(
            @Parameter(description = "Organization ID") @PathVariable UUID id) {
        return ResponseEntity.ok(organizationService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new organization")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Organization created"),
        @ApiResponse(responseCode = "400", description = "Validation error or duplicate name")
    })
    public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an organization")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Organization updated"),
        @ApiResponse(responseCode = "400", description = "Validation error or duplicate name"),
        @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<OrganizationResponse> update(
            @Parameter(description = "Organization ID") @PathVariable UUID id,
            @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(organizationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an organization (sets isActive = false)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Organization deactivated"),
        @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Organization ID") @PathVariable UUID id) {
        organizationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
