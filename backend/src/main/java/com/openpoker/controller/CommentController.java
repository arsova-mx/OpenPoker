package com.openpoker.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openpoker.entity.Comments;
import com.openpoker.entity.Ticket;
import com.openpoker.entity.User;
import com.openpoker.repository.CommentRepository;
import com.openpoker.repository.TicketRepository;
import com.openpoker.repository.UserRepository;
import com.openpoker.dto.CommentReponseDTO;


import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    // 🚀 POST /api/tickets/{ticketId}/comments -> Agregar un comentario
    @PostMapping
    public ResponseEntity<CommentReponseDTO> addComment(
            @AuthenticationPrincipal String username,
            @PathVariable UUID ticketId,
            @RequestBody MapCommentDTO dto) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado"));
                
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Comments comment = Comments.builder()
                .ticket(ticket)
                .user(user)
                .content(dto.getContent())
                .build();
        
        Comments savedComment = commentRepository.save(comment);

        CommentReponseDTO response = new CommentReponseDTO(
        savedComment.getId(), // Este ID ya viene poblado tras el guardado
        savedComment.getUser().getUsername(),
        savedComment.getContent(),
        savedComment.getCreatedAt() // Fecha automática generada por el @PrePersist
    );


        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 🚀 GET /api/tickets/{ticketId}/comments -> Ver la discusión del ticket
    @GetMapping
    public ResponseEntity<List<CommentReponseDTO>> getComments(@PathVariable UUID ticketId) {
        // 1. Obtenemos las entidades de la base de datos
        List<Comments> entities = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);

        // 2. Las mapeamos de forma limpia al DTO seguro
        List<CommentReponseDTO> dtoList = entities.stream()
                .map(c -> new CommentReponseDTO(
                    c.getId(),
                    c.getUser().getUsername(),
                    c.getContent(),
                    c.getCreatedAt()
                ))
                .toList();

        // 3. Devolvemos la lista protegida contra ciclos y fugas de datos
        return ResponseEntity.ok(dtoList);
    }

    @lombok.Data
    public static class MapCommentDTO {
        private String content;
    }

}
