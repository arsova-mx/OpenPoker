-- PostgreSQL no indexa automáticamente las columnas de llaves foráneas.
-- Estas son las que se consultan en cada evento de la sala (participantes, backlog, votos, comentarios).
create index if not exists idx_participants_game_session on participants (game_session_id);
create index if not exists idx_participants_user on participants (user_id);
create index if not exists idx_ticket_game_session on ticket (id_session);
create index if not exists idx_votes_ticket on votes (ticket_id);
create index if not exists idx_votes_participant on votes (participant_id);
create index if not exists idx_comments_ticket on comments (ticket_id);
create index if not exists idx_game_sessions_host on game_sessions (host_id);
