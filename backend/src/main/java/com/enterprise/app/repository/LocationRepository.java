package com.enterprise.app.repository;

import com.enterprise.app.entity.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {
    Page<Location> findByOrganizationId(UUID organizationId, Pageable pageable);
    boolean existsByNameIgnoreCaseAndOrganizationId(String name, UUID organizationId);
    boolean existsByNameIgnoreCaseAndOrganizationIdAndIdNot(String name, UUID organizationId, UUID excludeId);
}
