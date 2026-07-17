package com.openpoker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(
    name = "card_value",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_card_value_deck_order", columnNames = {"deck_id", "order_index"}),
        @UniqueConstraint(name = "uk_card_value_deck_value", columnNames = {"deck_id", "value"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardValue {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36)
    private UUID id;

    @Column(nullable = false)
    private String value;


    @ManyToOne
    @JoinColumn(name = "deck_id")
    //@JsonIgnore
    private VotingDeck deck;

    @Column(nullable = false)
    private Integer orderIndex;

    @Column(name="weight")
    private Integer weight;
}
