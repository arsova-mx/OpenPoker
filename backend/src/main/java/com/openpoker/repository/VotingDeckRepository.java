package com.openpoker.repository;

import com.openpoker.entity.VotingDeck;
import com.openpoker.model.CardSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VotingDeckRepository extends JpaRepository<VotingDeck, UUID> {
    Optional<VotingDeck> findByName(String name);
    Optional<VotingDeck> findByNameIgnoreCase(String name);
    Optional<VotingDeck> findBySeriesType(CardSeries seriesType);
    @Query("SELECT d FROM VotingDeck d LEFT JOIN FETCH d.cardValues WHERE d.seriesType = :seriesType")
    Optional<VotingDeck> findBySeriesTypeWithCards(@Param("seriesType") CardSeries seriesType);
}
