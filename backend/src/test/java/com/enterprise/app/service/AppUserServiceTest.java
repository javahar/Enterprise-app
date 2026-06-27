package com.enterprise.app.service;

import com.enterprise.app.dto.AppUserRequest;
import com.enterprise.app.dto.AppUserResponse;
import com.enterprise.app.dto.PageResponse;
import com.enterprise.app.entity.AppUser;
import com.enterprise.app.entity.Organization;
import com.enterprise.app.repository.AppUserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private AppUserService appUserService;

    private Organization buildOrg(UUID orgId) {
        Organization org = Organization.builder().name("Acme").build();
        org.setId(orgId);
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());
        return org;
    }

    private AppUser buildUser(Organization org, String email, boolean isActive) {
        AppUser user = AppUser.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email(email)
                .organization(org)
                .build();
        user.setId(UUID.randomUUID());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(isActive);
        return user;
    }

    private AppUserRequest request(String email, Boolean isActive) {
        return new AppUserRequest("Jane", "Smith", email, isActive);
    }

    // ── getAllByOrg ────────────────────────────────────────────────────────────

    @Test
    void getAllByOrg_returnsPageOfAllUsers_whenOrgIsActive() {
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        AppUser active = buildUser(org, "active@example.com", true);
        AppUser inactive = buildUser(org, "inactive@example.com", false);
        when(organizationService.findEntityOrThrow(orgId)).thenReturn(org);
        when(appUserRepository.findByOrganizationId(eq(orgId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(active, inactive)));

        PageResponse<AppUserResponse> result = appUserService.getAllByOrg(orgId, 0, 20);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content()).extracting(AppUserResponse::email)
                .containsExactlyInAnyOrder("active@example.com", "inactive@example.com");
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void getAllByOrg_throwsEntityNotFoundException_whenOrgNotFoundOrInactive() {
        UUID orgId = UUID.randomUUID();
        when(organizationService.findEntityOrThrow(orgId))
                .thenThrow(new EntityNotFoundException("Organization not found: " + orgId));

        assertThatThrownBy(() -> appUserService.getAllByOrg(orgId, 0, 20))
                .isInstanceOf(EntityNotFoundException.class);

        verify(appUserRepository, never()).findByOrganizationId(any(), any());
    }

    // ── getById ────────────────────────────────────────────────────────────────

    @Test
    void getById_returnsUser_whenFound() {
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        AppUser user = buildUser(org, "jane@example.com", true);
        when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));

        AppUserResponse result = appUserService.getById(user.getId());

        assertThat(result.id()).isEqualTo(user.getId());
        assertThat(result.email()).isEqualTo("jane@example.com");
        assertThat(result.organizationId()).isEqualTo(orgId);
    }

    @Test
    void getById_returnsInactiveUser_whenFound() {
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        AppUser user = buildUser(org, "jane@example.com", false);
        when(appUserRepository.findById(user.getId())).thenReturn(Optional.of(user));

        AppUserResponse result = appUserService.getById(user.getId());

        assertThat(result.isActive()).isFalse();
    }

    @Test
    void getById_throwsEntityNotFoundException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(appUserRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.getById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_savesAndReturns_whenOrgActiveAndEmailUnique() {
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        AppUser saved = buildUser(org, "jane@example.com", true);
        when(organizationService.findEntityOrThrow(orgId)).thenReturn(org);
        when(appUserRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(appUserRepository.save(any())).thenReturn(saved);

        AppUserResponse result = appUserService.create(orgId, request("jane@example.com", null));

        assertThat(result.email()).isEqualTo("jane@example.com");
        assertThat(result.isActive()).isTrue();
        verify(appUserRepository).save(any(AppUser.class));
    }

    @Test
    void create_throwsEntityNotFoundException_whenOrgNotFoundOrInactive() {
        UUID orgId = UUID.randomUUID();
        when(organizationService.findEntityOrThrow(orgId))
                .thenThrow(new EntityNotFoundException("Organization not found: " + orgId));

        assertThatThrownBy(() -> appUserService.create(orgId, request("jane@example.com", null)))
                .isInstanceOf(EntityNotFoundException.class);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void create_throwsIllegalArgumentException_whenEmailAlreadyExists() {
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        when(organizationService.findEntityOrThrow(orgId)).thenReturn(org);
        when(appUserRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> appUserService.create(orgId, request("jane@example.com", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jane@example.com");

        verify(appUserRepository, never()).save(any());
    }

    // ── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_updatesAndReturns_whenFoundAndEmailChanges() {
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        AppUser existing = buildUser(org, "jane@example.com", true);
        AppUser updated = buildUser(org, "new@example.com", true);
        updated.setId(existing.getId());

        when(appUserRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(appUserRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(appUserRepository.save(any())).thenReturn(updated);

        AppUserResponse result = appUserService.update(existing.getId(), request("new@example.com", null));

        assertThat(result.email()).isEqualTo("new@example.com");
    }

    @Test
    void update_doesNotCheckDuplicateEmail_whenEmailIsUnchanged() {
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        AppUser existing = buildUser(org, "jane@example.com", true);

        when(appUserRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(appUserRepository.save(any())).thenReturn(existing);

        appUserService.update(existing.getId(), request("jane@example.com", null));

        verify(appUserRepository, never()).existsByEmailIgnoreCase(anyString());
    }

    @Test
    void update_throwsEntityNotFoundException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(appUserRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.update(id, request("jane@example.com", null)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void update_throwsIllegalArgumentException_whenEmailConflicts() {
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        AppUser existing = buildUser(org, "jane@example.com", true);

        when(appUserRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(appUserRepository.existsByEmailIgnoreCase("other@example.com")).thenReturn(true);

        assertThatThrownBy(() -> appUserService.update(existing.getId(), request("other@example.com", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("other@example.com");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void update_setsIsActiveFalse_whenIsActiveProvided() {
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        AppUser existing = buildUser(org, "jane@example.com", true);
        AppUser deactivated = buildUser(org, "jane@example.com", false);
        deactivated.setId(existing.getId());

        when(appUserRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(appUserRepository.save(any())).thenReturn(deactivated);

        AppUserResponse result = appUserService.update(existing.getId(), request("jane@example.com", false));

        assertThat(result.isActive()).isFalse();
    }

    @Test
    void update_doesNotChangeIsActive_whenIsActiveIsNull() {
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        AppUser existing = buildUser(org, "jane@example.com", true);

        when(appUserRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(appUserRepository.save(any())).thenReturn(existing);

        appUserService.update(existing.getId(), request("jane@example.com", null));

        assertThat(existing.isActive()).isTrue();
    }

    // ── delete ─────────────────────────────────────────────────────────────────

    @Test
    void delete_hardDeletesUser_whenFound() {
        UUID id = UUID.randomUUID();
        when(appUserRepository.existsById(id)).thenReturn(true);

        appUserService.delete(id);

        verify(appUserRepository).deleteById(id);
    }

    @Test
    void delete_throwsEntityNotFoundException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(appUserRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> appUserService.delete(id))
                .isInstanceOf(EntityNotFoundException.class);

        verify(appUserRepository, never()).deleteById(any());
    }
}
