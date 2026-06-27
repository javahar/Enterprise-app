package com.enterprise.app.controller;

import com.enterprise.app.dto.LocationRequest;
import com.enterprise.app.dto.LocationResponse;
import com.enterprise.app.dto.PageResponse;
import com.enterprise.app.service.LocationService;
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

@WebMvcTest(LocationController.class)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LocationService locationService;

    @Autowired
    private ObjectMapper objectMapper;

    private LocationResponse response(UUID id, UUID orgId, String name) {
        return new LocationResponse(id, orgId, name, "123 Main St", "Springfield", "IL", "62701",
                LocalDateTime.now(), LocalDateTime.now());
    }

    private LocationRequest validRequest(String name) {
        return new LocationRequest(name, "123 Main St", "Springfield", "IL", "62701");
    }

    private PageResponse<LocationResponse> page(LocationResponse... items) {
        List<LocationResponse> list = List.of(items);
        return new PageResponse<>(list, 0, 20, list.size(), 1, true);
    }

    // ── GET /api/organizations/{orgId}/locations ───────────────────────────────

    @Test
    void getAllByOrg_returns200WithPage() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID locId = UUID.randomUUID();
        when(locationService.getAllByOrg(eq(orgId), eq(0), eq(20)))
                .thenReturn(page(response(locId, orgId, "Downtown")));

        mockMvc.perform(get("/api/organizations/{orgId}/locations", orgId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Downtown"))
                .andExpect(jsonPath("$.content[0].organizationId").value(orgId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAllByOrg_returns404WhenOrgNotFoundOrInactive() throws Exception {
        UUID orgId = UUID.randomUUID();
        when(locationService.getAllByOrg(eq(orgId), anyInt(), anyInt()))
                .thenThrow(new EntityNotFoundException("Organization not found: " + orgId));

        mockMvc.perform(get("/api/organizations/{orgId}/locations", orgId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── POST /api/organizations/{orgId}/locations ──────────────────────────────

    @Test
    void create_returns201WithCreatedLocation() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID locId = UUID.randomUUID();
        when(locationService.create(eq(orgId), any())).thenReturn(response(locId, orgId, "Downtown"));

        mockMvc.perform(post("/api/organizations/{orgId}/locations", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("Downtown"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(locId.toString()))
                .andExpect(jsonPath("$.name").value("Downtown"))
                .andExpect(jsonPath("$.organizationId").value(orgId.toString()));
    }

    @Test
    void create_returns400WhenNameIsBlank() throws Exception {
        UUID orgId = UUID.randomUUID();

        mockMvc.perform(post("/api/organizations/{orgId}/locations", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void create_returns400WhenDuplicateNameInOrg() throws Exception {
        UUID orgId = UUID.randomUUID();
        when(locationService.create(eq(orgId), any()))
                .thenThrow(new IllegalArgumentException("Location with name 'Downtown' already exists in this organization"));

        mockMvc.perform(post("/api/organizations/{orgId}/locations", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("Downtown"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Location with name 'Downtown' already exists in this organization"));
    }

    @Test
    void create_returns404WhenOrgNotFoundOrInactive() throws Exception {
        UUID orgId = UUID.randomUUID();
        when(locationService.create(eq(orgId), any()))
                .thenThrow(new EntityNotFoundException("Organization not found: " + orgId));

        mockMvc.perform(post("/api/organizations/{orgId}/locations", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("Downtown"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── GET /api/locations/{id} ────────────────────────────────────────────────

    @Test
    void getById_returns200WhenFound() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID locId = UUID.randomUUID();
        when(locationService.getById(locId)).thenReturn(response(locId, orgId, "Downtown"));

        mockMvc.perform(get("/api/locations/{id}", locId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(locId.toString()))
                .andExpect(jsonPath("$.name").value("Downtown"));
    }

    @Test
    void getById_returns404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(locationService.getById(id))
                .thenThrow(new EntityNotFoundException("Location not found: " + id));

        mockMvc.perform(get("/api/locations/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── PUT /api/locations/{id} ────────────────────────────────────────────────

    @Test
    void update_returns200WithUpdatedLocation() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID locId = UUID.randomUUID();
        when(locationService.update(eq(locId), any())).thenReturn(response(locId, orgId, "Uptown"));

        mockMvc.perform(put("/api/locations/{id}", locId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("Uptown"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Uptown"));
    }

    @Test
    void update_returns404WhenNotFound() throws Exception {
        UUID locId = UUID.randomUUID();
        when(locationService.update(eq(locId), any()))
                .thenThrow(new EntityNotFoundException("Location not found: " + locId));

        mockMvc.perform(put("/api/locations/{id}", locId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("Uptown"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void update_returns400WhenNameIsBlank() throws Exception {
        UUID locId = UUID.randomUUID();

        mockMvc.perform(put("/api/locations/{id}", locId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    // ── DELETE /api/locations/{id} ─────────────────────────────────────────────

    @Test
    void delete_returns204WhenDeleted() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(locationService).delete(id);

        mockMvc.perform(delete("/api/locations/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Location not found: " + id))
                .when(locationService).delete(id);

        mockMvc.perform(delete("/api/locations/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
