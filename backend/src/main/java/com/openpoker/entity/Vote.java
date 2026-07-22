package com.openpoker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "votes", uniqueConstraints = @UniqueConstraint(columnNames = {"ticket_id", "participant_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JdbcTypeCode(SqlTypes.VARCHAR) // ESTO ES LA CLAVE
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
