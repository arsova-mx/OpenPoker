package com.openpoker.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.openpoker.entity.CardValue;
import com.openpoker.entity.VotingDeck;


public interface CardValueRepository extends JpaRepository<CardValue, UUID>{
    List<CardValue> findByDeck(VotingDeck deck);
}
