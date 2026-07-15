package com.openpoker.entity;

import java.sql.Timestamp;
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
@Table(name = "game_sessions")
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

    @Column(name = "host_id", nullable = false,length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID hostUserId;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = new Timestamp(System.currentTimeMillis());
        updatedAt = new Timestamp(System.currentTimeMillis());
        if(status == null) {
            status = SessionStatus.WAITING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }

    @Column(nullable = false)
    private boolean votesRevealed = false;

    @ManyToOne
    @JoinColumn(name = "deck_id")
    private VotingDeck deck;
}
