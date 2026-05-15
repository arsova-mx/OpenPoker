package com.openpoker.service;

import com.openpoker.dto.VotingDeckResponse;
import com.openpoker.entity.DeckValue;
import com.openpoker.entity.VotingDeck;
import com.openpoker.globalexception.DeckNotFoundException;
import com.openpoker.model.CardSeries;
import com.openpoker.repository.VotingDeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardDeckService {
    private final VotingDeckRepository deckRepository;

    public List<VotingDeckResponse> getAllDecks() {
        return deckRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public VotingDeckResponse getDeckBySeriesType(CardSeries seriesType) {
        VotingDeck deck = deckRepository.findBySeriesType(seriesType)
                .orElseThrow(() -> new DeckNotFoundException("Deck no encontrado para tipo: " + seriesType));
        return mapToResponse(deck);
    }

    private VotingDeckResponse mapToResponse(VotingDeck deck) {
        List<String> values = deck.getValues() != null
                ? deck.getValues().stream().map(DeckValue::getValue).toList()
                : List.of();
        String seriesType = deck.getSeriesType() != null ? deck.getSeriesType().name() : null;
        return new VotingDeckResponse(deck.getId(), deck.getName(), seriesType, deck.getDescription(), values);
    }
}
