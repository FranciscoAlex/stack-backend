-- Migration: Add views column to posts table
-- Date: 2024

ALTER TABLE posts ADD COLUMN IF NOT EXISTS views INTEGER NOT NULL DEFAULT 0;

-- Update existing posts to have 0 views if they don't have one
UPDATE posts SET views = 0 WHERE views IS NULL;

