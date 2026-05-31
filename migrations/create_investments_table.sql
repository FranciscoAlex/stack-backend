-- Migration to create investments table for multiple BODIVAS investments per user
-- This replaces the single investment fields in the users table

-- Create investments table
CREATE TABLE IF NOT EXISTS investments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    bodivas_company_code VARCHAR(10) NOT NULL,
    bodivas_company_name VARCHAR(255) NOT NULL,
    shares_owned BIGINT NOT NULL,
    share_percentage DECIMAL(5,2),
    investment_amount DECIMAL(15,2) NOT NULL,
    entry_date DATE,
    current_value DECIMAL(15,2),
    average_price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_investment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create index for faster queries
CREATE INDEX IF NOT EXISTS idx_investments_user_id ON investments(user_id);
CREATE INDEX IF NOT EXISTS idx_investments_company_code ON investments(bodivas_company_code);

