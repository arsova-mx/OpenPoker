package com.openpoker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "voting_deck")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotingDeck {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<DeckValue> values;
}
