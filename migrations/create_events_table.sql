-- Migration to create events table for event publishing and management

-- Create events table
CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    location VARCHAR(500),
    event_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    image_url VARCHAR(500),
    category VARCHAR(100),
    max_attendees INTEGER,
    current_attendees INTEGER DEFAULT 0,
    price DECIMAL(10,2) DEFAULT 0.00,
    is_online BOOLEAN DEFAULT FALSE,
    online_link VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'
    is_published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_events_user_id ON events(user_id);
CREATE INDEX IF NOT EXISTS idx_events_status ON events(status);
CREATE INDEX IF NOT EXISTS idx_events_event_date ON events(event_date);
CREATE INDEX IF NOT EXISTS idx_events_is_published ON events(is_published);
CREATE INDEX IF NOT EXISTS idx_events_category ON events(category);

