package com.enterprise.app.repository;

import com.enterprise.app.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {
    List<Location> findByOrganizationId(UUID organizationId);
    List<Location> findByOrganizationIdAndIsActiveTrue(UUID organizationId);
}
