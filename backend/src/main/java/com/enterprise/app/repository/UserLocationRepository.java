package com.enterprise.app.repository;

import com.enterprise.app.entity.UserLocation;
import com.enterprise.app.entity.UserLocation.UserLocationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, UserLocationId> {

    List<UserLocation> findByUserId(UUID userId);

    List<UserLocation> findByLocationId(UUID locationId);

    @Query("SELECT ul FROM UserLocation ul JOIN FETCH ul.location WHERE ul.user.id = :userId")
    List<UserLocation> findWithLocationByUserId(@Param("userId") UUID userId);
}
