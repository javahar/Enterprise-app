package com.enterprise.app.controller;

import com.enterprise.app.dto.AppUserRequest;
import com.enterprise.app.dto.AppUserResponse;
import com.enterprise.app.dto.PageResponse;
import com.enterprise.app.service.AppUserService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppUserController.class)
class AppUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppUserService appUserService;

    @Autowired
    private ObjectMapper objectMapper;

    private AppUserResponse response(UUID id, UUID orgId, boolean isActive) {
        return new AppUserResponse(id, orgId, "Jane", "Smith", "jane@example.com",
                isActive, LocalDateTime.now(), LocalDateTime.now());
    }

    private AppUserRequest validRequest() {
        return new AppUserRequest("Jane", "Smith", "jane@example.com", null);
    }

    private PageResponse<AppUserResponse> page(AppUserResponse... items) {
        List<AppUserResponse> list = List.of(items);
        return new PageResponse<>(list, 0, 20, list.size(), 1, true);
    }

    // ── GET /api/organizations/{orgId}/users ───────────────────────────────────

    @Test
    void getAllByOrg_returns200WithPageOfActiveAndInactiveUsers() throws Exception {
        UUID orgId = UUID.randomUUID();
        when(appUserService.getAllByOrg(eq(orgId), eq(0), eq(20)))
                .thenReturn(page(response(UUID.randomUUID(), orgId, true), response(UUID.randomUUID(), orgId, false)));

        mockMvc.perform(get("/api/organizations/{orgId}/users", orgId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].organizationId").value(orgId.toString()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getAllByOrg_returns404WhenOrgNotFoundOrInactive() throws Exception {
        UUID orgId = UUID.randomUUID();
        when(appUserService.getAllByOrg(eq(orgId), anyInt(), anyInt()))
                .thenThrow(new EntityNotFoundException("Organization not found: " + orgId));

        mockMvc.perform(get("/api/organizations/{orgId}/users", orgId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── POST /api/organizations/{orgId}/users ──────────────────────────────────

    @Test
    void create_returns201WithCreatedUser() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(appUserService.create(eq(orgId), any())).thenReturn(response(userId, orgId, true));

        mockMvc.perform(post("/api/organizations/{orgId}/users", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void create_returns400WhenFirstNameIsBlank() throws Exception {
        UUID orgId = UUID.randomUUID();
        AppUserRequest request = new AppUserRequest("", "Smith", "jane@example.com", null);

        mockMvc.perform(post("/api/organizations/{orgId}/users", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists());
    }

    @Test
    void create_returns400WhenEmailIsInvalid() throws Exception {
        UUID orgId = UUID.randomUUID();
        AppUserRequest request = new AppUserRequest("Jane", "Smith", "not-an-email", null);

        mockMvc.perform(post("/api/organizations/{orgId}/users", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void create_returns400WhenEmailAlreadyExists() throws Exception {
        UUID orgId = UUID.randomUUID();
        when(appUserService.create(eq(orgId), any()))
                .thenThrow(new IllegalArgumentException("User with email 'jane@example.com' already exists"));

        mockMvc.perform(post("/api/organizations/{orgId}/users", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User with email 'jane@example.com' already exists"));
    }

    @Test
    void create_returns404WhenOrgNotFoundOrInactive() throws Exception {
        UUID orgId = UUID.randomUUID();
        when(appUserService.create(eq(orgId), any()))
                .thenThrow(new EntityNotFoundException("Organization not found: " + orgId));

        mockMvc.perform(post("/api/organizations/{orgId}/users", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── GET /api/users/{id} ────────────────────────────────────────────────────

    @Test
    void getById_returns200WhenFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(appUserService.getById(userId)).thenReturn(response(userId, orgId, true));

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"));
    }

    @Test
    void getById_returns200ForInactiveUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(appUserService.getById(userId)).thenReturn(response(userId, orgId, false));

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void getById_returns404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(appUserService.getById(id))
                .thenThrow(new EntityNotFoundException("User not found: " + id));

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── PUT /api/users/{id} ────────────────────────────────────────────────────

    @Test
    void update_returns200WithUpdatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(appUserService.update(eq(userId), any())).thenReturn(response(userId, orgId, true));

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }

    @Test
    void update_returns200WhenDeactivatingUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        AppUserRequest request = new AppUserRequest("Jane", "Smith", "jane@example.com", false);
        when(appUserService.update(eq(userId), any())).thenReturn(response(userId, orgId, false));

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void update_returns404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(appUserService.update(eq(id), any()))
                .thenThrow(new EntityNotFoundException("User not found: " + id));

        mockMvc.perform(put("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void update_returns400WhenEmailIsInvalid() throws Exception {
        UUID id = UUID.randomUUID();
        AppUserRequest request = new AppUserRequest("Jane", "Smith", "bad-email", null);

        mockMvc.perform(put("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    // ── DELETE /api/users/{id} ─────────────────────────────────────────────────

    @Test
    void delete_returns204WhenDeleted() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(appUserService).delete(id);

        mockMvc.perform(delete("/api/users/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new EntityNotFoundException("User not found: " + id))
                .when(appUserService).delete(id);

        mockMvc.perform(delete("/api/users/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
