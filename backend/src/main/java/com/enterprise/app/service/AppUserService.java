package com.enterprise.app.service;

import com.enterprise.app.dto.AppUserRequest;
import com.enterprise.app.dto.AppUserResponse;
import com.enterprise.app.dto.PageResponse;
import com.enterprise.app.entity.AppUser;
import com.enterprise.app.entity.Organization;
import com.enterprise.app.repository.AppUserRepository;
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
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final OrganizationService organizationService;

    @Transactional(readOnly = true)
    public PageResponse<AppUserResponse> getAllByOrg(UUID orgId, int page, int size) {
        log.debug("Fetching users for org: orgId={}, page={}, size={}", orgId, page, size);
        organizationService.findEntityOrThrow(orgId);
        return PageResponse.of(
                appUserRepository.findByOrganizationId(orgId, PageRequest.of(page, size))
                        .map(this::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public AppUserResponse getById(UUID id) {
        log.debug("Fetching user: id={}", id);
        return appUserRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    public AppUserResponse create(UUID orgId, AppUserRequest request) {
        Organization org = organizationService.findEntityOrThrow(orgId);
        if (appUserRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException(
                    "User with email '" + request.email() + "' already exists");
        }
        AppUser user = AppUser.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(request.email().trim())
                .organization(org)
                .build();
        AppUserResponse response = toResponse(appUserRepository.save(user));
        log.info("User created: id={}, email={}, orgId={}", response.id(), response.email(), orgId);
        return response;
    }

    public AppUserResponse update(UUID id, AppUserRequest request) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

        if (!user.getEmail().equalsIgnoreCase(request.email())
                && appUserRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException(
                    "User with email '" + request.email() + "' already exists");
        }

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(request.email().trim());
        if (request.isActive() != null) {
            user.setActive(request.isActive());
        }
        AppUserResponse response = toResponse(appUserRepository.save(user));
        log.info("User updated: id={}, email={}, isActive={}", id, response.email(), response.isActive());
        return response;
    }

    public void delete(UUID id) {
        if (!appUserRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found: " + id);
        }
        appUserRepository.deleteById(id);
        log.info("User hard-deleted: id={}", id);
    }

    private AppUserResponse toResponse(AppUser user) {
        return new AppUserResponse(
                user.getId(),
                user.getOrganization().getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
