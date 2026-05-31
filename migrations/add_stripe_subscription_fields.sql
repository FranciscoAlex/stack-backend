-- Migration: Add Stripe subscription fields to users table
-- Date: 2025-12-13
-- Description: Adds fields for Stripe payment integration and subscription management

-- Add Stripe customer ID
ALTER TABLE users ADD COLUMN IF NOT EXISTS stripe_customer_id VARCHAR(255);

-- Add Stripe subscription ID
ALTER TABLE users ADD COLUMN IF NOT EXISTS stripe_subscription_id VARCHAR(255);

-- Add subscription status
ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_status VARCHAR(50);

-- Add subscription plan type
ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_plan_type VARCHAR(50);

-- Add subscription current period end
ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_current_period_end TIMESTAMP;

-- Add subscription cancel at period end flag
ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_cancel_at_period_end BOOLEAN DEFAULT FALSE;

-- Create index for Stripe customer ID lookups
CREATE INDEX IF NOT EXISTS idx_users_stripe_customer_id ON users(stripe_customer_id);

-- Create index for subscription status lookups
CREATE INDEX IF NOT EXISTS idx_users_subscription_status ON users(subscription_status);

