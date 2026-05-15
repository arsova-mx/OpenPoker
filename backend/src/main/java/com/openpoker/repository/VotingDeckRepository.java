package com.openpoker.repository;

import com.openpoker.entity.VotingDeck;
import com.openpoker.model.CardSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VotingDeckRepository extends JpaRepository<VotingDeck, UUID> {
    Optional<VotingDeck> findByName(String name);
    Optional<VotingDeck> findByNameIgnoreCase(String name);
    Optional<VotingDeck> findBySeriesType(CardSeries seriesType);
}
