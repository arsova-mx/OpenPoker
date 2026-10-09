package com.openpoker.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.openpoker.entity.GameSession;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {

    Optional<GameSession> findBySessionCode(String sessionCode);

    // 1. Todas las sesiones donde el usuario fue Host O donde estuvo como Participante
    @Query("""
        SELECT DISTINCT gs FROM GameSession gs
        LEFT JOIN Participant p ON p.gameSession = gs
        WHERE gs.host.id = :userId OR p.user.id = :userId
        ORDER BY gs.createdAt DESC
    """)
    Page<GameSession> findUserSessionHistory(@Param("userId") UUID userId, Pageable pageable);

    // 2. Solo sesiones donde fue Host (creadas por él)
    Page<GameSession> findByHostIdOrderByCreatedAtDesc(UUID hostId, Pageable pageable);

    // 3. Solo sesiones donde participó como votante (y no fue el host)
    @Query("""
        SELECT DISTINCT p.gameSession FROM Participant p
        WHERE p.user.id = :userId AND p.gameSession.host.id != :userId
        ORDER BY p.gameSession.createdAt DESC
    """)
    Page<GameSession> findSessionsWhereUserParticipated(@Param("userId") UUID userId, Pageable pageable);
}
