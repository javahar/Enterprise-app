-- V4__fix_role_columns.sql
-- role_id was defined as SMALLINT FK to access_role, but Hibernate stores
-- the enum name (READ/WRITE/ADMIN) as VARCHAR via @Enumerated(EnumType.STRING).
-- This migration aligns the DB columns with what Hibernate expects.

-- Drop the FK constraints first, then change the column types
ALTER TABLE user_location
    DROP CONSTRAINT IF EXISTS user_location_role_id_fkey,
    ALTER COLUMN role_id TYPE VARCHAR(50) USING (
        CASE role_id
            WHEN 1 THEN 'READ'
            WHEN 2 THEN 'WRITE'
            WHEN 3 THEN 'ADMIN'
        END
    );

ALTER TABLE user_node_assignment
    DROP CONSTRAINT IF EXISTS user_node_assignment_role_id_fkey,
    ALTER COLUMN role_id TYPE VARCHAR(50) USING (
        CASE role_id
            WHEN 1 THEN 'READ'
            WHEN 2 THEN 'WRITE'
            WHEN 3 THEN 'ADMIN'
        END
    );

-- access_role lookup table is no longer needed;
-- the Java enum is the source of truth for valid values.
-- Keep the table but remove the FK dependency (already dropped above).
-- Optionally drop it entirely:
-- DROP TABLE access_role;
