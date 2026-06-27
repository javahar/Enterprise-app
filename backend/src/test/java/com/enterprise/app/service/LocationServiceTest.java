package com.enterprise.app.service;

import com.enterprise.app.dto.LocationRequest;
import com.enterprise.app.dto.LocationResponse;
import com.enterprise.app.dto.PageResponse;
import com.enterprise.app.entity.Location;
import com.enterprise.app.entity.Organization;
import com.enterprise.app.repository.LocationRepository;
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
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private LocationService locationService;

    private Organization buildOrg(UUID orgId) {
        Organization org = Organization.builder().name("Acme").build();
        org.setId(orgId);
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());
        return org;
    }

    private Location buildLocation(Organization org, String name) {
        Location loc = Location.builder()
                .name(name)
                .address("123 Main St")
                .city("Springfield")
                .stateCode("IL")
                .zip("62701")
                .organization(org)
                .build();
        loc.setId(UUID.randomUUID());
        loc.setCreatedAt(LocalDateTime.now());
        loc.setUpdatedAt(LocalDateTime.now());
        return loc;
    }

    private LocationRequest request(String name) {
        return new LocationRequest(name, "123 Main St", "Springfield", "IL", "62701");
    }

    // ── getAllByOrg ────────────────────────────────────────────────────────────

    @Test
    void getAllByOrg_returnsPageOfLocations_whenOrgIsActive() {
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        Location loc = buildLocation(org, "Downtown");
        when(organizationService.findEntityOrThrow(orgId)).thenReturn(org);
        when(locationRepository.findByOrganizationId(eq(orgId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(loc)));

        PageResponse<LocationResponse> result = locationService.getAllByOrg(orgId, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("Downtown");
        assertThat(result.content().get(0).organizationId()).isEqualTo(orgId);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getAllByOrg_throwsEntityNotFoundException_whenOrgNotFoundOrInactive() {
        UUID orgId = UUID.randomUUID();
        when(organizationService.findEntityOrThrow(orgId))
                .thenThrow(new EntityNotFoundException("Organization not found: " + orgId));

        assertThatThrownBy(() -> locationService.getAllByOrg(orgId, 0, 20))
                .isInstanceOf(EntityNotFoundException.class);

        verify(locationRepository, never()).findByOrganizationId(any(), any());
    }

    // ── getById ────────────────────────────────────────────────────────────────

    @Test
    void getById_returnsLocation_whenFound() {
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        Location loc = buildLocation(org, "Downtown");
        when(locationRepository.findById(loc.getId())).thenReturn(Optional.of(loc));

        LocationResponse result = locationService.getById(loc.getId());

        assertThat(result.id()).isEqualTo(loc.getId());
        assertThat(result.name()).isEqualTo("Downtown");
        assertThat(result.organizationId()).isEqualTo(orgId);
    }

    @Test
    void getById_throwsEntityNotFoundException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(locationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.getById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_savesAndReturns_whenOrgActiveAndNameUnique() {
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        Location saved = buildLocation(org, "Downtown");
        when(organizationService.findEntityOrThrow(orgId)).thenReturn(org);
        when(locationRepository.existsByNameIgnoreCaseAndOrganizationId("Downtown", orgId)).thenReturn(false);
        when(locationRepository.save(any())).thenReturn(saved);

        LocationResponse result = locationService.create(orgId, request("Downtown"));

        assertThat(result.name()).isEqualTo("Downtown");
        assertThat(result.organizationId()).isEqualTo(orgId);
        verify(locationRepository).save(any(Location.class));
    }

    @Test
    void create_throwsEntityNotFoundException_whenOrgNotFoundOrInactive() {
        UUID orgId = UUID.randomUUID();
        when(organizationService.findEntityOrThrow(orgId))
                .thenThrow(new EntityNotFoundException("Organization not found: " + orgId));

        assertThatThrownBy(() -> locationService.create(orgId, request("Downtown")))
                .isInstanceOf(EntityNotFoundException.class);

        verify(locationRepository, never()).save(any());
    }

    @Test
    void create_throwsIllegalArgumentException_whenNameAlreadyExistsInOrg() {
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        when(organizationService.findEntityOrThrow(orgId)).thenReturn(org);
        when(locationRepository.existsByNameIgnoreCaseAndOrganizationId("Downtown", orgId)).thenReturn(true);

        assertThatThrownBy(() -> locationService.create(orgId, request("Downtown")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Downtown");

        verify(locationRepository, never()).save(any());
    }

    // ── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_updatesAndReturns_whenFoundAndNameChanges() {
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        Location existing = buildLocation(org, "Downtown");
        Location updated = buildLocation(org, "Uptown");
        updated.setId(existing.getId());

        when(locationRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(locationRepository.existsByNameIgnoreCaseAndOrganizationIdAndIdNot(
                "Uptown", orgId, existing.getId())).thenReturn(false);
        when(locationRepository.save(any())).thenReturn(updated);

        LocationResponse result = locationService.update(existing.getId(), request("Uptown"));

        assertThat(result.name()).isEqualTo("Uptown");
    }

    @Test
    void update_doesNotCheckDuplicateName_whenNameIsUnchanged() {
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        Location existing = buildLocation(org, "Downtown");

        when(locationRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(locationRepository.save(any())).thenReturn(existing);

        locationService.update(existing.getId(), request("Downtown"));

        verify(locationRepository, never()).existsByNameIgnoreCaseAndOrganizationIdAndIdNot(
                anyString(), any(), any());
    }

    @Test
    void update_throwsEntityNotFoundException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(locationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.update(id, request("Uptown")))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void update_throwsIllegalArgumentException_whenNameConflictsWithinOrg() {
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        Location existing = buildLocation(org, "Downtown");

        when(locationRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(locationRepository.existsByNameIgnoreCaseAndOrganizationIdAndIdNot(
                "Uptown", orgId, existing.getId())).thenReturn(true);

        assertThatThrownBy(() -> locationService.update(existing.getId(), request("Uptown")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Uptown");

        verify(locationRepository, never()).save(any());
    }

    // ── delete ─────────────────────────────────────────────────────────────────

    @Test
    void delete_hardDeletesLocation_whenFound() {
        UUID id = UUID.randomUUID();
        when(locationRepository.existsById(id)).thenReturn(true);

        locationService.delete(id);

        verify(locationRepository).deleteById(id);
    }

    @Test
    void delete_throwsEntityNotFoundException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(locationRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> locationService.delete(id))
                .isInstanceOf(EntityNotFoundException.class);

        verify(locationRepository, never()).deleteById(any());
    }
}
