package com.openpoker.repository;

import com.openpoker.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByInviteCode(String inviteCode);
}
