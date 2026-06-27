package com.enterprise.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hierarchy_node")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HierarchyNode extends BaseEntity {

    @Column(nullable = false)
    private String name;

    /**
     * 1 = Region, 2 = State, 3 = County/City
     * Stored as SMALLINT in DB; Java Short maps to SMALLINT correctly.
     * DB CHECK constraint backs this up.
     */
    @Column(nullable = false)
    private Short level;

    /**
     * Self-referencing: a State's parent is a Region, a County's parent is a State.
     * Regions have no parent (null).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private HierarchyNode parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HierarchyNode> children = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
}
