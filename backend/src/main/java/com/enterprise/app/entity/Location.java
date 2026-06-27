package com.enterprise.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String address;

    private String city;

    @Column(name = "state_code")
    private String stateCode;

    private String zip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
