package com.openpoker.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.openpoker.entity.Comments;
import com.openpoker.entity.Participant;
@Repository
public interface CommentRepository extends JpaRepository<Comments, UUID>{

    List<Comments> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);

    List<Comments> findByUserId(UUID userId);
}
