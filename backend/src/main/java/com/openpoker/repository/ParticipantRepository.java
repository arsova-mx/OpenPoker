package com.openpoker.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.openpoker.entity.GameSession;
import com.openpoker.entity.Participant;
import com.openpoker.entity.User;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, UUID> {
    Optional<Participant> findByGameSessionAndUser(GameSession gameSession, User user);
    java.util.List<Participant> findAllByGameSession(GameSession gameSession);
    long countByGameSession(GameSession gameSession);
    void deleteAllByGameSession(GameSession gameSession);
    Optional<Participant> findByGameSessionAndGuestDisplayName(GameSession gameSession, String guestDisplayName);
}
