package com.openpoker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
// ✅ 1. Actualizamos el constraint para incluir 'round' y le damos un nombre explícito
@Table(name = "votes", uniqueConstraints = {
    @UniqueConstraint(name = "uk_vote_ticket_participant_round", columnNames = {"ticket_id", "participant_id", "round"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name="ticket_id",nullable = false)
    private Ticket ticket;

    @ManyToOne(optional = false)
    @JoinColumn(name="participant_id", nullable = false)
    private Participant participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_value_id", nullable = false)
    private CardValue cardValue;

    // ✅ 2. Agregamos el campo de la ronda
    @Column(nullable = false)
    private Integer round;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = new Timestamp(System.currentTimeMillis());
        updatedAt = new Timestamp(System.currentTimeMillis());
        // Inicializamos round por defecto en 1 si no se envía
        if (this.round == null) {
            this.round = 1;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}