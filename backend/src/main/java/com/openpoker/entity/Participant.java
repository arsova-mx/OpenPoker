package com.openpoker.entity;

import java.sql.Timestamp;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "participants")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Participant {
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR) // ESTO ES LA CLAVE
    @Column(length = 36)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name="game_session_id", nullable = false)
    private GameSession gameSession;

    @ManyToOne(optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "guest_display_name", nullable = true)
    private String guestDisplayName;

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

    public String getEffectiveName() {
        if (this.user != null) {
            return this.user.getUsername();
        }
        return this.guestDisplayName != null ? this.guestDisplayName : "Invitado Anónimo";
    }
}
