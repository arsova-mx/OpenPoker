-- 1. Actualizaciones en GAME_SESSIONS
ALTER TABLE game_sessions ADD COLUMN status VARCHAR(50) DEFAULT 'ACTIVE' NOT NULL;
ALTER TABLE game_sessions ADD COLUMN closed_at TIMESTAMP;
ALTER TABLE game_sessions ADD CONSTRAINT fk_game_session_host FOREIGN KEY (host_id) REFERENCES users(id);

CREATE INDEX idx_session_host_created ON game_sessions(host_id, created_at);

-- 2. Actualizaciones en PARTICIPANTS
ALTER TABLE participants ADD COLUMN left_at TIMESTAMP;

ALTER TABLE participants ADD CONSTRAINT uk_participant_user UNIQUE (game_session_id, user_id);
ALTER TABLE participants ADD CONSTRAINT uk_participant_guest UNIQUE (game_session_id, guest_display_name);

CREATE INDEX idx_participant_user_joined ON participants(user_id, joined_at);

-- 3. Actualizaciones en TICKET
ALTER TABLE ticket DROP COLUMN estimated_value;

ALTER TABLE ticket ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE ticket ADD COLUMN position INTEGER;
ALTER TABLE ticket ADD COLUMN current_round INTEGER DEFAULT 1 NOT NULL;
ALTER TABLE ticket ADD COLUMN finished_at TIMESTAMP;
ALTER TABLE ticket ADD COLUMN estimated_card_id VARCHAR(36); 

ALTER TABLE ticket ADD CONSTRAINT fk_ticket_estimated_card FOREIGN KEY (estimated_card_id) REFERENCES card_value(id);

-- 4. Actualizaciones en VOTES
ALTER TABLE votes DROP INDEX UK974vwejdcfaredqjkvwiow98j; 

ALTER TABLE votes ADD COLUMN round INTEGER DEFAULT 1 NOT NULL;

ALTER TABLE votes ADD CONSTRAINT uk_vote_ticket_participant_round UNIQUE (ticket_id, participant_id, round);