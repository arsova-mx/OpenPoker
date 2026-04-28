package com.openpoker.entity;

import java.sql.Timestamp;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "participants")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Participant {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    private GameSession gameSession;

    @ManyToOne(optional = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private Role role;

    private Timestamp joinedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        joinedAt = new Timestamp(System.currentTimeMillis());
    }

    public enum Role {
        HOST, VOTER
    }
}
