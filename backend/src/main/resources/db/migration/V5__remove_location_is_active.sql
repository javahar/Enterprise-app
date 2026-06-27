-- V5__remove_location_is_active.sql
-- Location uses hard delete; is_active is not needed
ALTER TABLE location DROP COLUMN is_active;
