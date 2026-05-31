-- Migration to remove username column from users table
-- This migration removes the username column since we're using firstName and lastName instead

ALTER TABLE users DROP COLUMN IF EXISTS username;

