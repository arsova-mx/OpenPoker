package com.openpoker.repository;

import com.openpoker.entity.Participantt;
import com.openpoker.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ParticipanttRepository extends JpaRepository<Participantt, UUID> {
    List<Participantt> findBySession(Session session);
}
