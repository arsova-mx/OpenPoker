package com.openpoker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Getter
@Setter
public class SessionParticipant {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String displayName;

    @ManyToOne
    private Session session;

    private Timestamp joinedAt;

    @PrePersist
    public void prePersist() {
        joinedAt = new Timestamp(System.currentTimeMillis());
    }
}
