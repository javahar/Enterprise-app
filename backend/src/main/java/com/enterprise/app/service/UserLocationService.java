package com.enterprise.app.service;

import com.enterprise.app.dto.LocationUserCountResponse;
import com.enterprise.app.dto.PageResponse;
import com.enterprise.app.dto.UserLocationRequest;
import com.enterprise.app.dto.UserLocationResponse;
import com.enterprise.app.entity.AppUser;
import com.enterprise.app.entity.Location;
import com.enterprise.app.entity.UserLocation;
import com.enterprise.app.entity.UserLocation.UserLocationId;
import com.enterprise.app.repository.AppUserRepository;
import com.enterprise.app.repository.LocationRepository;
import com.enterprise.app.repository.UserLocationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserLocationService {

    private final UserLocationRepository userLocationRepository;
    private final AppUserRepository appUserRepository;
    private final LocationRepository locationRepository;

    @Transactional(readOnly = true)
    public PageResponse<UserLocationResponse> getLocationsForUser(UUID userId, int page, int size) {
        log.debug("Fetching location assignments for user: userId={}, page={}, size={}", userId, page, size);
        if (!appUserRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found: " + userId);
        }
        return PageResponse.of(
                userLocationRepository.findWithLocationByUserId(userId, PageRequest.of(page, size))
                        .map(this::toResponse)
        );
    }

    public UserLocationResponse assign(UUID userId, UserLocationRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new EntityNotFoundException("Location not found: " + request.locationId()));

        if (!user.getOrganization().getId().equals(location.getOrganization().getId())) {
            throw new IllegalArgumentException(
                    "User and location must belong to the same organization");
        }

        UserLocationId id = new UserLocationId(userId, request.locationId());
        UserLocation userLocation = userLocationRepository.findById(id)
                .orElse(UserLocation.builder()
                        .id(id)
                        .user(user)
                        .location(location)
                        .build());
        userLocation.setRole(request.role());
        userLocationRepository.save(userLocation);
        log.info("User-location assignment upserted: userId={}, locationId={}, role={}", userId, request.locationId(), request.role());

        return new UserLocationResponse(
                userId,
                location.getId(),
                location.getName(),
                location.getCity(),
                location.getStateCode(),
                userLocation.getRole(),
                userLocation.getAssignedAt());
    }

    public void removeAssignment(UUID userId, UUID locationId) {
        UserLocationId id = new UserLocationId(userId, locationId);
        if (!userLocationRepository.existsById(id)) {
            throw new EntityNotFoundException(
                    "Assignment not found for user " + userId + " and location " + locationId);
        }
        userLocationRepository.deleteById(id);
        log.info("User-location assignment removed: userId={}, locationId={}", userId, locationId);
    }

    @Transactional(readOnly = true)
    public LocationUserCountResponse countUsersAtLocation(UUID locationId) {
        log.debug("Counting users at location: locationId={}", locationId);
        if (!locationRepository.existsById(locationId)) {
            throw new EntityNotFoundException("Location not found: " + locationId);
        }
        return new LocationUserCountResponse(userLocationRepository.countByLocationId(locationId));
    }

    private UserLocationResponse toResponse(UserLocation ul) {
        Location loc = ul.getLocation();
        return new UserLocationResponse(
                ul.getId().getUserId(),
                loc.getId(),
                loc.getName(),
                loc.getCity(),
                loc.getStateCode(),
                ul.getRole(),
                ul.getAssignedAt());
    }
}
