package com.enterprise.app.controller;

import com.enterprise.app.dto.LocationUserCountResponse;
import com.enterprise.app.dto.PageResponse;
import com.enterprise.app.dto.UserLocationRequest;
import com.enterprise.app.dto.UserLocationResponse;
import com.enterprise.app.entity.AccessRole;
import com.enterprise.app.service.UserLocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserLocationController.class)
class UserLocationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private UserLocationService userLocationService;
    @Autowired private ObjectMapper objectMapper;

    private UserLocationResponse response(UUID userId, UUID locationId, AccessRole role) {
        return new UserLocationResponse(userId, locationId, "Downtown", "Springfield", "IL",
                role, LocalDateTime.now());
    }

    private PageResponse<UserLocationResponse> page(UserLocationResponse... items) {
        List<UserLocationResponse> list = List.of(items);
        return new PageResponse<>(list, 0, 20, list.size(), 1, true);
    }

    // ── GET /api/users/{userId}/locations ──────────────────────────────────────

    @Test
    void getLocationsForUser_returns200WithPage() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(userLocationService.getLocationsForUser(eq(userId), eq(0), eq(20)))
                .thenReturn(page(response(userId, locationId, AccessRole.READ)));

        mockMvc.perform(get("/api/users/{userId}/locations", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].locationName").value("Downtown"))
                .andExpect(jsonPath("$.content[0].role").value("READ"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getLocationsForUser_returns200WithEmptyPage() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userLocationService.getLocationsForUser(eq(userId), eq(0), eq(20)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/users/{userId}/locations", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getLocationsForUser_returns404WhenUserNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userLocationService.getLocationsForUser(eq(userId), anyInt(), anyInt()))
                .thenThrow(new EntityNotFoundException("User not found: " + userId));

        mockMvc.perform(get("/api/users/{userId}/locations", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── POST /api/users/{userId}/locations ─────────────────────────────────────

    @Test
    void assign_returns200WithAssignment() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UserLocationRequest request = new UserLocationRequest(locationId, AccessRole.WRITE);
        when(userLocationService.assign(eq(userId), any()))
                .thenReturn(response(userId, locationId, AccessRole.WRITE));

        mockMvc.perform(post("/api/users/{userId}/locations", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("WRITE"))
                .andExpect(jsonPath("$.locationId").value(locationId.toString()));
    }

    @Test
    void assign_returns200OnRoleUpdate() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UserLocationRequest request = new UserLocationRequest(locationId, AccessRole.ADMIN);
        when(userLocationService.assign(eq(userId), any()))
                .thenReturn(response(userId, locationId, AccessRole.ADMIN));

        mockMvc.perform(post("/api/users/{userId}/locations", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void assign_returns400WhenLocationIdMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UserLocationRequest request = new UserLocationRequest(null, AccessRole.READ);

        mockMvc.perform(post("/api/users/{userId}/locations", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.locationId").exists());
    }

    @Test
    void assign_returns400WhenRoleMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UserLocationRequest request = new UserLocationRequest(UUID.randomUUID(), null);

        mockMvc.perform(post("/api/users/{userId}/locations", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.role").exists());
    }

    @Test
    void assign_returns404WhenUserNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UserLocationRequest request = new UserLocationRequest(UUID.randomUUID(), AccessRole.READ);
        when(userLocationService.assign(eq(userId), any()))
                .thenThrow(new EntityNotFoundException("User not found: " + userId));

        mockMvc.perform(post("/api/users/{userId}/locations", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void assign_returns400WhenDifferentOrg() throws Exception {
        UUID userId = UUID.randomUUID();
        UserLocationRequest request = new UserLocationRequest(UUID.randomUUID(), AccessRole.READ);
        when(userLocationService.assign(eq(userId), any()))
                .thenThrow(new IllegalArgumentException("User and location must belong to the same organization"));

        mockMvc.perform(post("/api/users/{userId}/locations", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User and location must belong to the same organization"));
    }

    // ── DELETE /api/users/{userId}/locations/{locationId} ──────────────────────

    @Test
    void removeAssignment_returns204WhenRemoved() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        doNothing().when(userLocationService).removeAssignment(userId, locationId);

        mockMvc.perform(delete("/api/users/{userId}/locations/{locationId}", userId, locationId))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeAssignment_returns404WhenNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Assignment not found"))
                .when(userLocationService).removeAssignment(userId, locationId);

        mockMvc.perform(delete("/api/users/{userId}/locations/{locationId}", userId, locationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── GET /api/locations/{locationId}/users/count ────────────────────────────

    @Test
    void countUsersAtLocation_returns200WithCount() throws Exception {
        UUID locationId = UUID.randomUUID();
        when(userLocationService.countUsersAtLocation(locationId))
                .thenReturn(new LocationUserCountResponse(5L));

        mockMvc.perform(get("/api/locations/{locationId}/users/count", locationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    void countUsersAtLocation_returns200WithZero_whenNoUsersAssigned() throws Exception {
        UUID locationId = UUID.randomUUID();
        when(userLocationService.countUsersAtLocation(locationId))
                .thenReturn(new LocationUserCountResponse(0L));

        mockMvc.perform(get("/api/locations/{locationId}/users/count", locationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void countUsersAtLocation_returns404WhenLocationNotFound() throws Exception {
        UUID locationId = UUID.randomUUID();
        when(userLocationService.countUsersAtLocation(locationId))
                .thenThrow(new EntityNotFoundException("Location not found: " + locationId));

        mockMvc.perform(get("/api/locations/{locationId}/users/count", locationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
