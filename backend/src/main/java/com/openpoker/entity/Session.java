package com.openpoker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "sessions")
@Getter
@Setter
public class Session {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String inviteCode;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    public enum Status {
        WAITING, VOTING, REVEALED, CLOSED
    }

    @PrePersist
    public void prePersist() {
        createdAt = new Timestamp(System.currentTimeMillis());
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
