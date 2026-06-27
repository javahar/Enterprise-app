-- V3__user_node_assignment_schema.sql
-- Phase 3: UserNodeAssignment — assign a user to a hierarchy node with a role
--   The role propagates to all locations under that node (additive, no conflicts)

CREATE TABLE user_node_assignment (
    user_id UUID     NOT NULL REFERENCES app_user(id)       ON DELETE CASCADE,
    node_id UUID     NOT NULL REFERENCES hierarchy_node(id) ON DELETE CASCADE,
    role_id SMALLINT NOT NULL REFERENCES access_role(id),
    assigned_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, node_id)
);

CREATE INDEX idx_una_user ON user_node_assignment(user_id);
CREATE INDEX idx_una_node ON user_node_assignment(node_id);
