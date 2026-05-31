-- Migration: Add pinned column to posts table
-- Date: 2024

ALTER TABLE posts ADD COLUMN IF NOT EXISTS pinned BOOLEAN NOT NULL DEFAULT false;

-- Create index for pinned posts for better query performance
CREATE INDEX IF NOT EXISTS idx_posts_pinned ON posts(pinned);

