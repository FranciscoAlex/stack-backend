-- Migration to add activation token and password reset token fields to users table
-- This migration adds fields for account activation and password recovery functionality

-- Add activation token fields
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS activation_token VARCHAR(255),
ADD COLUMN IF NOT EXISTS activation_token_expires_at TIMESTAMP;

-- Add password reset token fields
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS password_reset_token VARCHAR(255),
ADD COLUMN IF NOT EXISTS password_reset_token_expires_at TIMESTAMP;

-- Update existing users to be enabled (for backward compatibility)
-- New registrations will require activation
UPDATE users SET enabled = true WHERE enabled IS NULL OR activation_token IS NULL;

