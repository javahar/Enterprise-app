-- V2__hierarchy_schema.sql
-- Phase 2: HierarchyNode (self-referencing tree) + NodeLocation join

-- ------------------------------------------------------------
-- HierarchyNode — self-referencing tree (Region/State/County)
--   level 1 = Region, level 2 = State, level 3 = County/City
-- ------------------------------------------------------------
CREATE TABLE hierarchy_node (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    level           SMALLINT NOT NULL CHECK (level IN (1, 2, 3)),
    parent_id       UUID REFERENCES hierarchy_node(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),

    -- A level-1 node (Region) must have no parent
    CONSTRAINT chk_level1_no_parent CHECK (
        level != 1 OR parent_id IS NULL
    ),
    -- A level-2 or level-3 node must have a parent
    CONSTRAINT chk_non_root_has_parent CHECK (
        level = 1 OR parent_id IS NOT NULL
    )
);

CREATE INDEX idx_hierarchy_node_parent ON hierarchy_node(parent_id);
CREATE INDEX idx_hierarchy_node_org    ON hierarchy_node(organization_id);
CREATE INDEX idx_hierarchy_node_level  ON hierarchy_node(level);

-- ------------------------------------------------------------
-- NodeLocation — which locations belong to which hierarchy node
--   Service layer enforces: cannot assign to level 3 unless
--   already in level 2; cannot assign to level 2 unless in level 1
-- ------------------------------------------------------------
CREATE TABLE node_location (
    node_id     UUID NOT NULL REFERENCES hierarchy_node(id) ON DELETE CASCADE,
    location_id UUID NOT NULL REFERENCES location(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (node_id, location_id)
);

CREATE INDEX idx_node_location_node     ON node_location(node_id);
CREATE INDEX idx_node_location_location ON node_location(location_id);
