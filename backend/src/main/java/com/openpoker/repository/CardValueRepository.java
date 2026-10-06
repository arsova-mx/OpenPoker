package com.openpoker.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openpoker.entity.CardValue;
import com.openpoker.entity.VotingDeck;

public interface CardValueRepository extends JpaRepository<CardValue, UUID> {
    
    List<CardValue> findByDeck(VotingDeck deck);

    @Query(value = "SELECT * FROM card_value c " +
                   "WHERE c.deck_id = :deckId AND c.weight IS NOT NULL " +
                   // En empate (ej. promedio 2.5 entre 2 y 3) se sugiere la carta mayor: estimación conservadora
                   "ORDER BY ABS(c.weight - :avgWeight) ASC, c.weight DESC LIMIT 1",
           nativeQuery = true)
    Optional<CardValue> findClosestByWeight(@Param("deckId") String deckId, @Param("avgWeight") double avgWeight);
}