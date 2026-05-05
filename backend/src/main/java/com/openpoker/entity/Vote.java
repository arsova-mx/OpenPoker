package com.openpoker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "votes", uniqueConstraints = @UniqueConstraint(columnNames = {"game_session_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    private GameSession gameSession;

    @ManyToOne(optional = false)
    private User user;

    @Column(nullable = false)
    private String cardValue;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = new Timestamp(System.currentTimeMillis());
        updatedAt = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }

}
