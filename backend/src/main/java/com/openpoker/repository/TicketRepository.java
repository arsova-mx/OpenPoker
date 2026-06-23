package com.openpoker.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.openpoker.entity.Participant;
import com.openpoker.entity.Ticket;
@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID>{

    List<Ticket> findByGameSessionId(UUID gameSessionId);

}
