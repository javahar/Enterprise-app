package com.enterprise.app.repository;

import com.enterprise.app.entity.UserLocation;
import com.enterprise.app.entity.UserLocation.UserLocationId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, UserLocationId> {

    @Query(value = "SELECT ul FROM UserLocation ul JOIN FETCH ul.location WHERE ul.user.id = :userId",
           countQuery = "SELECT COUNT(ul) FROM UserLocation ul WHERE ul.user.id = :userId")
    Page<UserLocation> findWithLocationByUserId(@Param("userId") UUID userId, Pageable pageable);

    long countByLocationId(UUID locationId);
}
