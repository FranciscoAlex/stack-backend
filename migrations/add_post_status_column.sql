-- Migration: Add status column to posts table
-- Date: 2024

ALTER TABLE posts ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';

-- Update existing posts to be APPROVED (for backward compatibility)
UPDATE posts SET status = 'APPROVED' WHERE status IS NULL;

