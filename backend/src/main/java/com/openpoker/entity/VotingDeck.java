package com.openpoker.entity;

import com.openpoker.model.CardSeries;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "series_type")
    private CardSeries seriesType;

    @Column(length = 500)
    private String description;

    @OrderBy("orderIndex ASC")
    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<CardValue> values;
}
