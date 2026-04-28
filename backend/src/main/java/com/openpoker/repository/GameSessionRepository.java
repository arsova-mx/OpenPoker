package com.openpoker.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.openpoker.entity.GameSession;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {
    Optional<GameSession> findBySessionCode(String sessionCode);
}
