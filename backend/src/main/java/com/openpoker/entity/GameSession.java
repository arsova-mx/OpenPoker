package com.openpoker.entity;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "game_sessions", indexes = {
    @Index(name = "idx_session_host_created", columnList = "host_id, created_at")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GameSession {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR) // ESTO ES LA CLAVE
    @Column(length = 36)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String sessionCode;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "closed_at")
    private Instant closedAt;


    private Timestamp createdAt;
    private Timestamp updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = new Timestamp(System.currentTimeMillis());
        updatedAt = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }

    @Column(nullable = false)
    private boolean votesRevealed = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "deck_id")
    private VotingDeck deck;
}
