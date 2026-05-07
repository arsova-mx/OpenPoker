package com.openpoker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "deck_value")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeckValue {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String value;

    @ManyToOne
    @JoinColumn(name = "deck_id")
    private VotingDeck deck;
}
