-- V1__baseline_schema.sql
-- Phase 1 core tables: organization, location, user, access_role, user_location

-- ------------------------------------------------------------
-- Organizations
-- ------------------------------------------------------------
CREATE TABLE organization (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- ------------------------------------------------------------
-- Locations (belong to an organization)
-- ------------------------------------------------------------
CREATE TABLE location (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    address         VARCHAR(500),
    city            VARCHAR(100),
    state_code      VARCHAR(50),
    zip             VARCHAR(20),
    organization_id UUID NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_location_org ON location(organization_id);

-- ------------------------------------------------------------
-- Users (belong to an organization)
-- ------------------------------------------------------------
CREATE TABLE app_user (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    organization_id UUID NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_org   ON app_user(organization_id);
CREATE INDEX idx_user_email ON app_user(email);

-- ------------------------------------------------------------
-- Access roles (lookup table)
-- ------------------------------------------------------------
CREATE TABLE access_role (
    id   SMALLINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO access_role (id, name) VALUES
    (1, 'READ'),
    (2, 'WRITE'),
    (3, 'ADMIN');

-- ------------------------------------------------------------
-- UserLocation — direct user ↔ location assignment (Phase 1)
-- ------------------------------------------------------------
CREATE TABLE user_location (
    user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    location_id UUID NOT NULL REFERENCES location(id) ON DELETE CASCADE,
    role_id     SMALLINT NOT NULL REFERENCES access_role(id),
    assigned_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, location_id)
);

CREATE INDEX idx_user_location_user     ON user_location(user_id);
CREATE INDEX idx_user_location_location ON user_location(location_id);
