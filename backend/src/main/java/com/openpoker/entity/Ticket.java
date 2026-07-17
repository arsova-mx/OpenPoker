package com.openpoker.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
@Table(name = "ticket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR) // ESTO ES LA CLAVE
    @Column(length = 36)
    private UUID id;

    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name="id_session", nullable = false)
    private GameSession gameSession;

    @Column(name="title", nullable = false,length = 255)
    private String tittle;

    @Column(name="description",length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    
    @ManyToOne
    @JoinColumn(name = "estimated_card_id") // Relación con el catálogo oficial
    private CardValue estimatedCard;

    private Integer estimatedValue;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = TicketStatus.WAITING; // <-- Inicia siempre en WAITING
        }
    }





    

}
