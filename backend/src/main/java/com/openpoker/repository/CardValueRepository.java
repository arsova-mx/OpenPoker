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

    @Query(value = "SELECT * FROM card_value WHERE deck_id = :deckId " +
       "ORDER BY ABS(weight - :avgWeight) ASC LIMIT 1", nativeQuery = true)
    CardValue findClosestByWeight(@Param("deckId") UUID deckId, @Param("avgWeight") double avgWeight);
}
