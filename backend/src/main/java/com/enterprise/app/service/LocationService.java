package com.enterprise.app.service;

import com.enterprise.app.dto.LocationRequest;
import com.enterprise.app.dto.LocationResponse;
import com.enterprise.app.dto.PageResponse;
import com.enterprise.app.entity.Location;
import com.enterprise.app.entity.Organization;
import com.enterprise.app.repository.LocationRepository;
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
public class LocationService {

    private final LocationRepository locationRepository;
    private final OrganizationService organizationService;

    @Transactional(readOnly = true)
    public PageResponse<LocationResponse> getAllByOrg(UUID orgId, int page, int size) {
        log.debug("Fetching locations for org: orgId={}, page={}, size={}", orgId, page, size);
        organizationService.findEntityOrThrow(orgId);
        return PageResponse.of(
                locationRepository.findByOrganizationId(orgId, PageRequest.of(page, size))
                        .map(this::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public LocationResponse getById(UUID id) {
        log.debug("Fetching location: id={}", id);
        return locationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Location not found: " + id));
    }

    public LocationResponse create(UUID orgId, LocationRequest request) {
        Organization org = organizationService.findEntityOrThrow(orgId);
        if (locationRepository.existsByNameIgnoreCaseAndOrganizationId(request.name(), orgId)) {
            throw new IllegalArgumentException(
                    "Location with name '" + request.name() + "' already exists in this organization");
        }
        Location location = Location.builder()
                .name(request.name().trim())
                .address(request.address())
                .city(request.city())
                .stateCode(request.stateCode())
                .zip(request.zip())
                .organization(org)
                .build();
        LocationResponse response = toResponse(locationRepository.save(location));
        log.info("Location created: id={}, name={}, orgId={}", response.id(), response.name(), orgId);
        return response;
    }

    public LocationResponse update(UUID id, LocationRequest request) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Location not found: " + id));

        if (!location.getName().equalsIgnoreCase(request.name())
                && locationRepository.existsByNameIgnoreCaseAndOrganizationIdAndIdNot(
                        request.name(), location.getOrganization().getId(), id)) {
            throw new IllegalArgumentException(
                    "Location with name '" + request.name() + "' already exists in this organization");
        }

        location.setName(request.name().trim());
        location.setAddress(request.address());
        location.setCity(request.city());
        location.setStateCode(request.stateCode());
        location.setZip(request.zip());
        LocationResponse response = toResponse(locationRepository.save(location));
        log.info("Location updated: id={}, name={}", id, response.name());
        return response;
    }

    public void delete(UUID id) {
        if (!locationRepository.existsById(id)) {
            throw new EntityNotFoundException("Location not found: " + id);
        }
        locationRepository.deleteById(id);
        log.info("Location hard-deleted: id={}", id);
    }

    private LocationResponse toResponse(Location loc) {
        return new LocationResponse(
                loc.getId(),
                loc.getOrganization().getId(),
                loc.getName(),
                loc.getAddress(),
                loc.getCity(),
                loc.getStateCode(),
                loc.getZip(),
                loc.getCreatedAt(),
                loc.getUpdatedAt());
    }
}
