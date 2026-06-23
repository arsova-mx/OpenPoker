package com.openpoker.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.openpoker.entity.Participant;
import com.openpoker.entity.Ticket;

public interface TicketRepository extends JpaRepository<Participant, UUID>{

    List<Ticket> findByGameSessionId(UUID gameSessionId);

}
