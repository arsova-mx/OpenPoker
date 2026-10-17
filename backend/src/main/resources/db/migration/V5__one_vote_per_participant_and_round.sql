-- #40: cada reinicio de la votación abre una ronda nueva (votes.round, V3). La restricción única de V1
-- (un voto por participante y ticket) seguía activa, así que el voto de la segunda ronda fallaba.
-- V3 ya creó la restricción correcta: uk_vote_ticket_participant_round (ticket_id, participant_id, round).
alter table votes drop constraint uk_votes_ticket_participant;
