package com.enterprise.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_node_assignment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNodeAssignment {

    @EmbeddedId
    private UserNodeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("nodeId")
    @JoinColumn(name = "node_id")
    private HierarchyNode node;

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
    public static class UserNodeId implements Serializable {
        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "node_id")
        private UUID nodeId;
    }
}
