-- Migration to add investment fields for BODIVAS companies to users table
-- This migration adds fields for tracking user investments in Angolan stock exchange

-- Add BODIVAS investment fields
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS bodivas_company_code VARCHAR(10),
ADD COLUMN IF NOT EXISTS bodivas_company_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS shares_owned BIGINT,
ADD COLUMN IF NOT EXISTS share_percentage DECIMAL(5,2),
ADD COLUMN IF NOT EXISTS investment_amount DECIMAL(15,2),
ADD COLUMN IF NOT EXISTS entry_date DATE,
ADD COLUMN IF NOT EXISTS current_value DECIMAL(15,2),
ADD COLUMN IF NOT EXISTS average_price DECIMAL(10,2);

