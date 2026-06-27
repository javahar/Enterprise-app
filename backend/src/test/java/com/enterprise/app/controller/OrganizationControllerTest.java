package com.enterprise.app.controller;

import com.enterprise.app.dto.OrganizationRequest;
import com.enterprise.app.dto.OrganizationResponse;
import com.enterprise.app.dto.PageResponse;
import com.enterprise.app.service.OrganizationService;
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

@WebMvcTest(OrganizationController.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationService organizationService;

    @Autowired
    private ObjectMapper objectMapper;

    private OrganizationResponse response(UUID id, String name, boolean isActive) {
        return new OrganizationResponse(id, name, "Desc", isActive, LocalDateTime.now(), LocalDateTime.now());
    }

    private PageResponse<OrganizationResponse> page(OrganizationResponse... items) {
        List<OrganizationResponse> list = List.of(items);
        return new PageResponse<>(list, 0, 20, list.size(), 1, true);
    }

    // ── GET /api/organizations ─────────────────────────────────────────────────

    @Test
    void getAll_returns200WithPage() throws Exception {
        UUID id = UUID.randomUUID();
        when(organizationService.getAll(0, 20)).thenReturn(page(response(id, "Acme", true)));

        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Acme"))
                .andExpect(jsonPath("$.content[0].isActive").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAll_returns200WithEmptyPage() throws Exception {
        when(organizationService.getAll(0, 20))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── GET /api/organizations/{id} ────────────────────────────────────────────

    @Test
    void getById_returns200WhenFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(organizationService.getById(id)).thenReturn(response(id, "Acme", true));

        mockMvc.perform(get("/api/organizations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Acme"));
    }

    @Test
    void getById_returns404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(organizationService.getById(id))
                .thenThrow(new EntityNotFoundException("Organization not found: " + id));

        mockMvc.perform(get("/api/organizations/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Organization not found: " + id));
    }

    // ── POST /api/organizations ────────────────────────────────────────────────

    @Test
    void create_returns201WithCreatedOrg() throws Exception {
        UUID id = UUID.randomUUID();
        OrganizationRequest request = new OrganizationRequest("Acme", "Desc", null);
        when(organizationService.create(any())).thenReturn(response(id, "Acme", true));

        mockMvc.perform(post("/api/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Acme"));
    }

    @Test
    void create_returns400WhenNameIsBlank() throws Exception {
        OrganizationRequest request = new OrganizationRequest("", "Desc", null);

        mockMvc.perform(post("/api/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void create_returns400WhenDuplicateName() throws Exception {
        OrganizationRequest request = new OrganizationRequest("Acme", null, null);
        when(organizationService.create(any()))
                .thenThrow(new IllegalArgumentException("Organization with name 'Acme' already exists"));

        mockMvc.perform(post("/api/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Organization with name 'Acme' already exists"));
    }

    // ── PUT /api/organizations/{id} ────────────────────────────────────────────

    @Test
    void update_returns200WithUpdatedOrg() throws Exception {
        UUID id = UUID.randomUUID();
        OrganizationRequest request = new OrganizationRequest("Acme Updated", "New Desc", null);
        when(organizationService.update(eq(id), any())).thenReturn(response(id, "Acme Updated", true));

        mockMvc.perform(put("/api/organizations/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Updated"));
    }

    @Test
    void update_returns404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        OrganizationRequest request = new OrganizationRequest("Acme", null, null);
        when(organizationService.update(eq(id), any()))
                .thenThrow(new EntityNotFoundException("Organization not found: " + id));

        mockMvc.perform(put("/api/organizations/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void update_returns400WhenNameIsBlank() throws Exception {
        UUID id = UUID.randomUUID();
        OrganizationRequest request = new OrganizationRequest("", null, null);

        mockMvc.perform(put("/api/organizations/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    // ── DELETE /api/organizations/{id} ─────────────────────────────────────────

    @Test
    void delete_returns204WhenDeleted() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(organizationService).delete(id);

        mockMvc.perform(delete("/api/organizations/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new EntityNotFoundException("Organization not found: " + id))
                .when(organizationService).delete(id);

        mockMvc.perform(delete("/api/organizations/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── Global exception handler ───────────────────────────────────────────────

    @Test
    void unexpectedException_returns500() throws Exception {
        when(organizationService.getAll(anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Something exploded"));

        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }
}
