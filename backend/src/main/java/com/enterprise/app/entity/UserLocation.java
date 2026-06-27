package com.enterprise.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLocation {

    @EmbeddedId
    private UserLocationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("locationId")
    @JoinColumn(name = "location_id")
    private Location location;

    /**
     * Stored as the numeric role_id (1/2/3) via a converter.
     * We use @Enumerated(STRING) against the lookup name for readability in JPQL,
     * but the DB column stores the smallint FK — handled by a column definition override.
     *
     * Simpler approach: store as String-named column mapping to access_role.name.
     * The FK to access_role is enforced by Flyway DDL; Hibernate sees it as a plain column.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role_id", nullable = false)
    private AccessRole role;

    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();

    // ----------------------------------------------------------------
    // Composite key
    // ----------------------------------------------------------------
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class UserLocationId implements Serializable {
        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "location_id")
        private UUID locationId;
    }
}
