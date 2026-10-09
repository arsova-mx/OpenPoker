package com.openpoker.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.openpoker.entity.Ticket;
import com.openpoker.entity.TicketStatus;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByGameSessionId(UUID gameSessionId);

    Optional<Ticket> findByIdAndGameSessionId(UUID ticketId, UUID sessionId);

    // 1. Obtener los tickets ordenados por su posición para la vista de detalle
    List<Ticket> findByGameSessionIdOrderByPositionAsc(UUID gameSessionId);

    // 2. Conteo total de tickets por sesión
    long countByGameSessionId(UUID gameSessionId);

    // 3. Conteo de tickets filtrados por estado (ej. FINISHED)
    long countByGameSessionIdAndStatus(UUID gameSessionId, TicketStatus status);
}