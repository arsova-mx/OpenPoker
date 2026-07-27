package com.openpoker.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.openpoker.entity.Participant;
import com.openpoker.entity.Ticket;
import com.openpoker.entity.Vote;

@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {
    long countByTicketId(UUID ticketId);
    Optional<Vote> findByTicketIdAndParticipantId(UUID ticketId, UUID participantId);
    Optional<Vote> findByTicketAndParticipant(Ticket ticket, Participant participant);
    List<Vote> findAllByTicket(Ticket ticket);
    void deleteAllByTicket(Ticket ticket);
    List<Vote> findAllByTicketId(UUID ticketId);
}
