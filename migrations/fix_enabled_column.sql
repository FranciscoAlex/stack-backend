-- Migration to fix the enabled column to ensure it's not nullable and has a proper default
-- This ensures account activation works correctly

-- First, update any NULL values to false
UPDATE users SET enabled = false WHERE enabled IS NULL;

-- Alter the column to set NOT NULL and DEFAULT false
ALTER TABLE users ALTER COLUMN enabled SET NOT NULL;
ALTER TABLE users ALTER COLUMN enabled SET DEFAULT false;

-- Ensure indexes exist for activation lookups
CREATE INDEX IF NOT EXISTS idx_users_activation_token ON users(activation_token) WHERE activation_token IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_password_reset_token ON users(password_reset_token) WHERE password_reset_token IS NOT NULL;

