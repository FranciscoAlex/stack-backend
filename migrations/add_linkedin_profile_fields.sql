-- Migration to add LinkedIn-like profile fields to users table
-- This migration adds fields for professional profile information

-- Add cover image URL
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS cover_image_url VARCHAR(500);

-- Add professional profile fields
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS headline VARCHAR(500),
ADD COLUMN IF NOT EXISTS company VARCHAR(255),
ADD COLUMN IF NOT EXISTS role VARCHAR(255),
ADD COLUMN IF NOT EXISTS about TEXT,
ADD COLUMN IF NOT EXISTS location VARCHAR(255),
ADD COLUMN IF NOT EXISTS website VARCHAR(500),
ADD COLUMN IF NOT EXISTS twitter VARCHAR(255),
ADD COLUMN IF NOT EXISTS linkedin VARCHAR(500);

