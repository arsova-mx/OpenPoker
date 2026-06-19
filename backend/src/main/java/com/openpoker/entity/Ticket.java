package com.openpoker.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    private UUID id;

    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name="id_session", nullable = false)
    private GameSession gameSession;

    @Column(name="title", nullable = false,length = 255)
    private String tittle;

    @Column(name="description",length = 500)
    private String description;






    

}
