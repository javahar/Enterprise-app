package com.enterprise.app.service;

import com.enterprise.app.dto.OrganizationRequest;
import com.enterprise.app.dto.OrganizationResponse;
import com.enterprise.app.dto.PageResponse;
import com.enterprise.app.entity.Organization;
import com.enterprise.app.repository.OrganizationRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private Organization buildOrg(String name, String description, boolean isActive) {
        Organization org = Organization.builder()
                .name(name)
                .description(description)
                .build();
        org.setId(UUID.randomUUID());
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());
        org.setActive(isActive);
        return org;
    }

    // ── getAll ─────────────────────────────────────────────────────────────────

    @Test
    void getAll_returnsPageOfActiveOrgs() {
        Organization org = buildOrg("Acme", "Test", true);
        when(organizationRepository.findByIsActiveTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(org)));

        PageResponse<OrganizationResponse> result = organizationService.getAll(0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("Acme");
        assertThat(result.content().get(0).isActive()).isTrue();
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getAll_returnsEmptyPage_whenNoActiveOrgs() {
        when(organizationRepository.findByIsActiveTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(organizationService.getAll(0, 20).content()).isEmpty();
    }

    // ── getById ────────────────────────────────────────────────────────────────

    @Test
    void getById_returnsResponse_whenFound() {
        Organization org = buildOrg("Acme", "Desc", true);
        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));

        OrganizationResponse result = organizationService.getById(org.getId());

        assertThat(result.id()).isEqualTo(org.getId());
        assertThat(result.name()).isEqualTo("Acme");
        assertThat(result.description()).isEqualTo("Desc");
    }

    @Test
    void getById_throwsEntityNotFoundException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(organizationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.getById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_savesAndReturns_whenNameIsUnique() {
        OrganizationRequest request = new OrganizationRequest("Acme", "Desc", null);
        Organization saved = buildOrg("Acme", "Desc", true);
        when(organizationRepository.existsByNameIgnoreCase("Acme")).thenReturn(false);
        when(organizationRepository.save(any())).thenReturn(saved);

        OrganizationResponse result = organizationService.create(request);

        assertThat(result.name()).isEqualTo("Acme");
        assertThat(result.isActive()).isTrue();
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    void create_throwsIllegalArgumentException_whenNameAlreadyExists() {
        OrganizationRequest request = new OrganizationRequest("Acme", null, null);
        when(organizationRepository.existsByNameIgnoreCase("Acme")).thenReturn(true);

        assertThatThrownBy(() -> organizationService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Acme");

        verify(organizationRepository, never()).save(any());
    }

    // ── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_updatesAndReturns_whenFoundAndNameChanges() {
        Organization existing = buildOrg("Acme", "Old", true);
        OrganizationRequest request = new OrganizationRequest("Acme Updated", "New", null);
        Organization updated = buildOrg("Acme Updated", "New", true);
        updated.setId(existing.getId());

        when(organizationRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(organizationRepository.existsByNameIgnoreCase("Acme Updated")).thenReturn(false);
        when(organizationRepository.save(any())).thenReturn(updated);

        OrganizationResponse result = organizationService.update(existing.getId(), request);

        assertThat(result.name()).isEqualTo("Acme Updated");
        assertThat(result.description()).isEqualTo("New");
    }

    @Test
    void update_doesNotCheckDuplicateName_whenNameIsUnchanged() {
        Organization existing = buildOrg("Acme", "Desc", true);
        OrganizationRequest request = new OrganizationRequest("Acme", "New Desc", null);

        when(organizationRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(organizationRepository.save(any())).thenReturn(existing);

        organizationService.update(existing.getId(), request);

        verify(organizationRepository, never()).existsByNameIgnoreCase(anyString());
    }

    @Test
    void update_throwsEntityNotFoundException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(organizationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.update(id, new OrganizationRequest("X", null, null)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void update_throwsIllegalArgumentException_whenNewNameConflicts() {
        Organization existing = buildOrg("Acme", "Desc", true);
        when(organizationRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(organizationRepository.existsByNameIgnoreCase("Beta")).thenReturn(true);

        assertThatThrownBy(() -> organizationService.update(existing.getId(), new OrganizationRequest("Beta", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Beta");

        verify(organizationRepository, never()).save(any());
    }

    @Test
    void update_setsIsActiveFalse_whenIsActiveProvided() {
        Organization existing = buildOrg("Acme", "Desc", true);
        OrganizationRequest request = new OrganizationRequest("Acme", "Desc", false);
        Organization deactivated = buildOrg("Acme", "Desc", false);
        deactivated.setId(existing.getId());

        when(organizationRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(organizationRepository.save(any())).thenReturn(deactivated);

        OrganizationResponse result = organizationService.update(existing.getId(), request);

        assertThat(result.isActive()).isFalse();
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    void update_doesNotChangeIsActive_whenIsActiveIsNull() {
        Organization existing = buildOrg("Acme", "Desc", true);
        OrganizationRequest request = new OrganizationRequest("Acme", "New Desc", null);

        when(organizationRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(organizationRepository.save(any())).thenReturn(existing);

        organizationService.update(existing.getId(), request);

        verify(organizationRepository).save(any(Organization.class));
        assertThat(existing.isActive()).isTrue();
    }

    // ── findEntityOrThrow ──────────────────────────────────────────────────────

    @Test
    void findEntityOrThrow_returnsOrg_whenFoundAndActive() {
        Organization org = buildOrg("Acme", "Desc", true);
        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));

        Organization result = organizationService.findEntityOrThrow(org.getId());

        assertThat(result.getId()).isEqualTo(org.getId());
        assertThat(result.getName()).isEqualTo("Acme");
    }

    @Test
    void findEntityOrThrow_throwsEntityNotFoundException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(organizationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.findEntityOrThrow(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void findEntityOrThrow_throwsEntityNotFoundException_whenOrgIsInactive() {
        Organization org = buildOrg("Acme", "Desc", false);
        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));

        assertThatThrownBy(() -> organizationService.findEntityOrThrow(org.getId()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── delete ─────────────────────────────────────────────────────────────────

    @Test
    void delete_setsIsActiveFalse_whenFound() {
        Organization org = buildOrg("Acme", "Desc", true);
        when(organizationRepository.findById(org.getId())).thenReturn(Optional.of(org));

        organizationService.delete(org.getId());

        assertThat(org.isActive()).isFalse();
        verify(organizationRepository).save(org);
    }

    @Test
    void delete_throwsEntityNotFoundException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(organizationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.delete(id))
                .isInstanceOf(EntityNotFoundException.class);

        verify(organizationRepository, never()).save(any());
    }
}
