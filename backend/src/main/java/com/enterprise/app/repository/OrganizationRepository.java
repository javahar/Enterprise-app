package com.enterprise.app.repository;

import com.enterprise.app.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    List<Organization> findByIsActiveTrue();
    boolean existsByNameIgnoreCase(String name);
}
