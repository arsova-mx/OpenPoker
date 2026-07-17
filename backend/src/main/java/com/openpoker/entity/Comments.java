package com.openpoker.entity;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comments {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36)
    private UUID id;

    @ManyToOne
    @JoinColumn(name="ticket_id",nullable = false)
    private Ticket ticket;

    @ManyToOne
    @JoinColumn(name="user_id",nullable = false)
    private User user;

    @Column(name="content",nullable = false)
    private String content;

    @Column(name="created_at",nullable = false,updatable = false)
    private Timestamp createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID(); // Genera el ID si tus servicios lo mandan nulo
        }
        this.createdAt = Timestamp.from(Instant.now()); // Inyecta la hora exacta del servidor
    }
}
