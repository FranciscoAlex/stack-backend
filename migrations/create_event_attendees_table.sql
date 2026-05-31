-- Migration to create event_attendees table for event enrollment tracking

-- Create event_attendees table
CREATE TABLE IF NOT EXISTS event_attendees (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    ticket_code VARCHAR(20) NOT NULL UNIQUE,
    status VARCHAR(20) DEFAULT 'CONFIRMED', -- 'CONFIRMED', 'CANCELLED', 'CHECKED_IN'
    checked_in_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_attendee_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_attendee_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_event_user UNIQUE (event_id, user_id)
);

-- Create indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_event_attendees_event_id ON event_attendees(event_id);
CREATE INDEX IF NOT EXISTS idx_event_attendees_user_id ON event_attendees(user_id);
CREATE INDEX IF NOT EXISTS idx_event_attendees_ticket_code ON event_attendees(ticket_code);
CREATE INDEX IF NOT EXISTS idx_event_attendees_status ON event_attendees(status);

