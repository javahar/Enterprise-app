package com.enterprise.app.service;

import com.enterprise.app.dto.OrganizationRequest;
import com.enterprise.app.dto.OrganizationResponse;
import com.enterprise.app.dto.PageResponse;
import com.enterprise.app.entity.Organization;
import com.enterprise.app.repository.OrganizationRepository;
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
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public PageResponse<OrganizationResponse> getAll(int page, int size) {
        log.debug("Fetching all active organizations: page={}, size={}", page, size);
        return PageResponse.of(
                organizationRepository.findByIsActiveTrue(PageRequest.of(page, size))
                        .map(this::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getById(UUID id) {
        log.debug("Fetching organization: id={}", id);
        return organizationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + id));
    }

    public OrganizationResponse create(OrganizationRequest request) {
        if (organizationRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException(
                    "Organization with name '" + request.name() + "' already exists");
        }
        Organization org = Organization.builder()
                .name(request.name().trim())
                .description(request.description())
                .build();
        OrganizationResponse response = toResponse(organizationRepository.save(org));
        log.info("Organization created: id={}, name={}", response.id(), response.name());
        return response;
    }

    public OrganizationResponse update(UUID id, OrganizationRequest request) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + id));

        if (!org.getName().equalsIgnoreCase(request.name())
                && organizationRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException(
                    "Organization with name '" + request.name() + "' already exists");
        }

        org.setName(request.name().trim());
        org.setDescription(request.description());
        if (request.isActive() != null) {
            org.setActive(request.isActive());
        }
        OrganizationResponse response = toResponse(organizationRepository.save(org));
        log.info("Organization updated: id={}, name={}, isActive={}", id, response.name(), response.isActive());
        return response;
    }

    public void delete(UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + id));
        org.setActive(false);
        organizationRepository.save(org);
        log.info("Organization soft-deleted: id={}", id);
    }

    // Package-private — used by other services (e.g. LocationService) that need the entity, not the DTO
    Organization findEntityOrThrow(UUID id) {
        return organizationRepository.findById(id)
                .filter(Organization::isActive)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + id));
    }

    private OrganizationResponse toResponse(Organization org) {
        return new OrganizationResponse(
                org.getId(),
                org.getName(),
                org.getDescription(),
                org.isActive(),
                org.getCreatedAt(),
                org.getUpdatedAt());
    }
}
