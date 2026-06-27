package com.enterprise.app.repository;

import com.enterprise.app.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Page<Organization> findByIsActiveTrue(Pageable pageable);
    boolean existsByNameIgnoreCase(String name);
}
