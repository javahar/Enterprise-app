package com.enterprise.app.repository;

import com.enterprise.app.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    List<AppUser> findByOrganizationId(UUID organizationId);
    List<AppUser> findByOrganizationIdAndIsActiveTrue(UUID organizationId);
    Optional<AppUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
