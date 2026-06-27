package com.enterprise.app.repository;

import com.enterprise.app.entity.HierarchyNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HierarchyNodeRepository extends JpaRepository<HierarchyNode, UUID> {

    // All root nodes (Regions) for an org
    List<HierarchyNode> findByOrganizationIdAndParentIsNull(UUID organizationId);

    // All nodes at a given level for an org
    List<HierarchyNode> findByOrganizationIdAndLevel(UUID organizationId, Short level);

    // Direct children of a node
    List<HierarchyNode> findByParentId(UUID parentId);

    // All nodes where parent belongs to org (used for subtree queries)
    @Query("SELECT n FROM HierarchyNode n WHERE n.organization.id = :orgId ORDER BY n.level, n.name")
    List<HierarchyNode> findAllByOrgOrdered(@Param("orgId") UUID orgId);
}
