package com.enterprise.app.service;

import com.enterprise.app.dto.LocationUserCountResponse;
import com.enterprise.app.dto.PageResponse;
import com.enterprise.app.dto.UserLocationRequest;
import com.enterprise.app.dto.UserLocationResponse;
import com.enterprise.app.entity.AccessRole;
import com.enterprise.app.entity.AppUser;
import com.enterprise.app.entity.Location;
import com.enterprise.app.entity.Organization;
import com.enterprise.app.entity.UserLocation;
import com.enterprise.app.entity.UserLocation.UserLocationId;
import com.enterprise.app.repository.AppUserRepository;
import com.enterprise.app.repository.LocationRepository;
import com.enterprise.app.repository.UserLocationRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLocationServiceTest {

    @Mock private UserLocationRepository userLocationRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private LocationRepository locationRepository;

    @InjectMocks
    private UserLocationService userLocationService;

    private Organization buildOrg(UUID orgId) {
        Organization org = Organization.builder().name("Acme").build();
        org.setId(orgId);
        return org;
    }

    private AppUser buildUser(UUID userId, Organization org) {
        AppUser user = AppUser.builder()
                .firstName("Jane").lastName("Smith").email("jane@example.com").organization(org).build();
        user.setId(userId);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private Location buildLocation(UUID locationId, Organization org) {
        Location loc = Location.builder()
                .name("Downtown").city("Springfield").stateCode("IL").organization(org).build();
        loc.setId(locationId);
        loc.setCreatedAt(LocalDateTime.now());
        loc.setUpdatedAt(LocalDateTime.now());
        return loc;
    }

    private UserLocation buildAssignment(AppUser user, Location loc, AccessRole role) {
        UserLocationId id = new UserLocationId(user.getId(), loc.getId());
        return UserLocation.builder().id(id).user(user).location(loc).role(role).build();
    }

    // ── getLocationsForUser ────────────────────────────────────────────────────

    @Test
    void getLocationsForUser_returnsPageOfAssignments_whenUserExists() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        AppUser user = buildUser(userId, org);
        Location loc = buildLocation(UUID.randomUUID(), org);
        UserLocation assignment = buildAssignment(user, loc, AccessRole.READ);

        when(appUserRepository.existsById(userId)).thenReturn(true);
        when(userLocationRepository.findWithLocationByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(assignment)));

        PageResponse<UserLocationResponse> result = userLocationService.getLocationsForUser(userId, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).userId()).isEqualTo(userId);
        assertThat(result.content().get(0).locationName()).isEqualTo("Downtown");
        assertThat(result.content().get(0).role()).isEqualTo(AccessRole.READ);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getLocationsForUser_returnsEmptyPage_whenUserHasNoAssignments() {
        UUID userId = UUID.randomUUID();
        when(appUserRepository.existsById(userId)).thenReturn(true);
        when(userLocationRepository.findWithLocationByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(userLocationService.getLocationsForUser(userId, 0, 20).content()).isEmpty();
    }

    @Test
    void getLocationsForUser_throwsEntityNotFoundException_whenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(appUserRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> userLocationService.getLocationsForUser(userId, 0, 20))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(userId.toString());

        verify(userLocationRepository, never()).findWithLocationByUserId(any(), any());
    }

    // ── assign ─────────────────────────────────────────────────────────────────

    @Test
    void assign_createsNewAssignment_whenNotPreviouslyAssigned() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        AppUser user = buildUser(userId, org);
        Location loc = buildLocation(locationId, org);
        UserLocationId id = new UserLocationId(userId, locationId);

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(loc));
        when(userLocationRepository.findById(id)).thenReturn(Optional.empty());
        when(userLocationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserLocationResponse result = userLocationService.assign(userId, new UserLocationRequest(locationId, AccessRole.WRITE));

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.locationId()).isEqualTo(locationId);
        assertThat(result.role()).isEqualTo(AccessRole.WRITE);
        verify(userLocationRepository).save(any(UserLocation.class));
    }

    @Test
    void assign_updatesRole_whenAssignmentAlreadyExists() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        AppUser user = buildUser(userId, org);
        Location loc = buildLocation(locationId, org);
        UserLocationId id = new UserLocationId(userId, locationId);
        UserLocation existing = buildAssignment(user, loc, AccessRole.READ);

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(loc));
        when(userLocationRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userLocationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserLocationResponse result = userLocationService.assign(userId, new UserLocationRequest(locationId, AccessRole.ADMIN));

        assertThat(result.role()).isEqualTo(AccessRole.ADMIN);
        verify(userLocationRepository).save(any(UserLocation.class));
    }

    @Test
    void assign_throwsEntityNotFoundException_whenUserNotFound() {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(appUserRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userLocationService.assign(userId, new UserLocationRequest(locationId, AccessRole.READ)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(userId.toString());

        verify(userLocationRepository, never()).save(any());
    }

    @Test
    void assign_throwsEntityNotFoundException_whenLocationNotFound() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        Organization org = buildOrg(orgId);
        AppUser user = buildUser(userId, org);

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(locationRepository.findById(locationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userLocationService.assign(userId, new UserLocationRequest(locationId, AccessRole.READ)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(locationId.toString());

        verify(userLocationRepository, never()).save(any());
    }

    @Test
    void assign_throwsIllegalArgumentException_whenUserAndLocationInDifferentOrgs() {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        Organization orgA = buildOrg(UUID.randomUUID());
        Organization orgB = buildOrg(UUID.randomUUID());
        AppUser user = buildUser(userId, orgA);
        Location loc = buildLocation(locationId, orgB);

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(loc));

        assertThatThrownBy(() -> userLocationService.assign(userId, new UserLocationRequest(locationId, AccessRole.READ)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same organization");

        verify(userLocationRepository, never()).save(any());
    }

    // ── removeAssignment ───────────────────────────────────────────────────────

    @Test
    void removeAssignment_deletesSuccessfully_whenExists() {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UserLocationId id = new UserLocationId(userId, locationId);
        when(userLocationRepository.existsById(id)).thenReturn(true);

        userLocationService.removeAssignment(userId, locationId);

        verify(userLocationRepository).deleteById(id);
    }

    @Test
    void removeAssignment_throwsEntityNotFoundException_whenNotFound() {
        UUID userId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UserLocationId id = new UserLocationId(userId, locationId);
        when(userLocationRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> userLocationService.removeAssignment(userId, locationId))
                .isInstanceOf(EntityNotFoundException.class);

        verify(userLocationRepository, never()).deleteById(any());
    }

    // ── countUsersAtLocation ───────────────────────────────────────────────────

    @Test
    void countUsersAtLocation_returnsCount_whenLocationExists() {
        UUID locationId = UUID.randomUUID();
        when(locationRepository.existsById(locationId)).thenReturn(true);
        when(userLocationRepository.countByLocationId(locationId)).thenReturn(3L);

        LocationUserCountResponse result = userLocationService.countUsersAtLocation(locationId);

        assertThat(result.count()).isEqualTo(3L);
    }

    @Test
    void countUsersAtLocation_returnsZero_whenNoUsersAssigned() {
        UUID locationId = UUID.randomUUID();
        when(locationRepository.existsById(locationId)).thenReturn(true);
        when(userLocationRepository.countByLocationId(locationId)).thenReturn(0L);

        assertThat(userLocationService.countUsersAtLocation(locationId).count()).isZero();
    }

    @Test
    void countUsersAtLocation_throwsEntityNotFoundException_whenLocationNotFound() {
        UUID locationId = UUID.randomUUID();
        when(locationRepository.existsById(locationId)).thenReturn(false);

        assertThatThrownBy(() -> userLocationService.countUsersAtLocation(locationId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(locationId.toString());
    }
}
