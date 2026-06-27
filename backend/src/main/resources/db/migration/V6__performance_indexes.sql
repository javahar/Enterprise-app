-- V6__performance_indexes.sql
-- Composite indexes for common filtered queries at scale

-- app_user: org-scoped active-user filter (used by findByOrganizationIdAndIsActiveTrue)
CREATE INDEX idx_user_org_active ON app_user(organization_id, is_active);

-- hierarchy_node: org + level filter (used by findByOrganizationIdAndLevel)
CREATE INDEX idx_hierarchy_node_org_level ON hierarchy_node(organization_id, level);
