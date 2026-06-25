package com.openpoker.service;

import com.openpoker.dto.VotingDeckResponse;
import com.openpoker.entity.VotingDeck;
import com.openpoker.globalexception.DeckNotFoundException;
import com.openpoker.model.CardSeries;
import com.openpoker.repository.VotingDeckRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardDeckServiceTest {

    @Mock
    VotingDeckRepository deckRepository;

    @InjectMocks
    CardDeckService cardDeckService;

    @Test
    void getAllDecks_returnsAllDecksWithValues() {
        VotingDeck fibonacci = VotingDeck.builder()
                .id(UUID.randomUUID())
                .name("Fibonacci")
                .seriesType(CardSeries.FIBONACCI)
                .description("Estimación relativa clásica")
                
                .build();

        VotingDeck tShirt = VotingDeck.builder()
                .id(UUID.randomUUID())
                .name("T-Shirt Sizes")
                .seriesType(CardSeries.T_SHIRT)
                .description("Estimación rápida sin números")
                
                .build();

        when(deckRepository.findAll()).thenReturn(List.of(fibonacci, tShirt));

        List<VotingDeckResponse> result = cardDeckService.getAllDecks();

        assertEquals(2, result.size());
        assertEquals("Fibonacci", result.get(0).name());
        assertEquals("FIBONACCI", result.get(0).seriesType());
        assertEquals("Estimación relativa clásica", result.get(0).description());
       
        assertEquals("T-Shirt Sizes", result.get(1).name());
        assertEquals("T_SHIRT", result.get(1).seriesType());
    }

    @Test
    void getDeckBySeriesType_returnsDeck() {
        VotingDeck dotVoting = VotingDeck.builder()
                .id(UUID.randomUUID())
                .name("Dot Voting")
                .seriesType(CardSeries.DOT_VOTING)
                .description("Votación simple de priorización")
               
                .build();

        when(deckRepository.findBySeriesType(CardSeries.DOT_VOTING)).thenReturn(Optional.of(dotVoting));

        VotingDeckResponse result = cardDeckService.getDeckBySeriesType(CardSeries.DOT_VOTING);

        assertEquals("Dot Voting", result.name());
        assertEquals("DOT_VOTING", result.seriesType());
       
    }

    @Test
    void getDeckBySeriesType_throwsWhenNotFound() {
        when(deckRepository.findBySeriesType(CardSeries.FIBONACCI)).thenReturn(Optional.empty());

        assertThrows(DeckNotFoundException.class, () -> cardDeckService.getDeckBySeriesType(CardSeries.FIBONACCI));
    }
}
