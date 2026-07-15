package com.openpoker.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openpoker.entity.CardValue;
import com.openpoker.entity.VotingDeck;


public interface CardValueRepository extends JpaRepository<CardValue, UUID>{
    List<CardValue> findByDeck(VotingDeck deck);

    @Query("SELECT c FROM CardValue c WHERE c.deck.id = :deckId AND c.weight > 0 " +
           "ORDER BY ABS(c.weight - :avgWeight) ASC LIMIT 1")
    CardValue findClosestByWeight(@Param("deckId") UUID deckId, @Param("avgWeight") double avgWeight);
}
