package com.openpoker.repository;

import com.openpoker.entity.GameSession;
import com.openpoker.entity.User;
import com.openpoker.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {
    Optional<Vote> findByGameSessionAndUser(GameSession gameSession, User user);
    List<Vote> findAllByGameSession(GameSession gameSession);
}
