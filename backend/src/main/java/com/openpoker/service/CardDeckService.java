package com.openpoker.service;

import com.openpoker.dto.CardValueResponse;
import com.openpoker.dto.VotingDeckResponse;
import com.openpoker.entity.CardValue;
import com.openpoker.entity.VotingDeck;
import com.openpoker.globalexception.DeckNotFoundException;
import com.openpoker.model.CardSeries;
import com.openpoker.repository.CardValueRepository;
import com.openpoker.repository.VotingDeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardDeckService {
    private final VotingDeckRepository deckRepository;
    private final CardValueRepository cardValueRepository;

    public List<VotingDeckResponse> getAllDecks() {
        return deckRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public VotingDeckResponse getDeckBySeriesType(CardSeries seriesType) {
        VotingDeck deck = deckRepository.findBySeriesTypeWithCards(seriesType)
                .orElseThrow(() -> new DeckNotFoundException("Deck no encontrado para tipo: " + seriesType));
        return mapToResponse(deck);
    }

    private VotingDeckResponse mapToResponse(VotingDeck deck) {
            // 🔒 Validación defensiva contra listas nulas
        List<CardValueResponse> cardResponses = deck.getCardValues() == null 
            ? List.of() // Si es nulo, asignamos una lista vacía inmutable de Java
            : deck.getCardValues().stream()
                .map(card -> new CardValueResponse(card.getId(), card.getValue(), card.getOrderIndex()))
                .toList();

        String seriesType = deck.getSeriesType() != null ? deck.getSeriesType().name() : null;

        return new VotingDeckResponse(
            deck.getId(), 
            deck.getName(), 
            seriesType, 
            deck.getDescription(), 
            cardResponses
    );
        

    }
}
