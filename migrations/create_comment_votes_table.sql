-- Create comment_votes table
CREATE TABLE IF NOT EXISTS comment_votes (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    vote_type VARCHAR(10) NOT NULL,
    CONSTRAINT fk_comment_vote_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_vote_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_comment_vote_user UNIQUE (comment_id, user_id),
    CONSTRAINT chk_vote_type CHECK (vote_type IN ('up', 'down'))
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_comment_votes_comment_id ON comment_votes(comment_id);
CREATE INDEX IF NOT EXISTS idx_comment_votes_user_id ON comment_votes(user_id);

