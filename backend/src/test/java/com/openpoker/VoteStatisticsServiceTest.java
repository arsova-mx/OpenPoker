package com.openpoker;

import com.openpoker.dto.VoteStatisticsDTO;
import com.openpoker.entity.CardValue;
import com.openpoker.entity.Vote;
import com.openpoker.service.VoteStatisticsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VoteStatisticsServiceTest {

    private VoteStatisticsService voteStatisticsService;

    @BeforeEach
    void setUp() {
        voteStatisticsService = new VoteStatisticsService();
    }

    // Método auxiliar para crear objetos Vote rápidamente en memoria
    private Vote createVote(int weight) {
        CardValue cardValue = new CardValue();
        cardValue.setWeight(weight);
        
        Vote vote = new Vote();
        vote.setId(UUID.randomUUID());
        vote.setCardValue(cardValue);
        return vote;
    }

    @Test
    @DisplayName("Debe calcular 100% de consenso cuando todos votan lo mismo")
    void shouldCalculateFullConsensus() {
        // Given: 3 votos de peso 5
        List<Vote> votes = List.of(createVote(5), createVote(5), createVote(5));

        // When: Calculamos las estadísticas
        VoteStatisticsDTO stats = voteStatisticsService.calculateStatistics(votes);

        // Then: Verificamos los resultados
        assertEquals(5.0, stats.average());
        assertEquals(100.0, stats.consensusPercentage());
        assertTrue(stats.isFullConsensus());
        assertTrue(stats.outlierVoteIds().isEmpty());
    }

    @Test
    @DisplayName("Debe detectar outliers correctamente")
    void shouldDetectOutliers() {
        // Given: Cuatro votos de 2 y un voto disparado de 13
        Vote vote1 = createVote(2);
        Vote vote2 = createVote(2);
        Vote vote3 = createVote(2);
        Vote vote4 = createVote(2);
        Vote outlierVote = createVote(13); // Outlier claro

        List<Vote> votes = List.of(vote1, vote2, vote3, vote4, outlierVote);

        // When
        VoteStatisticsDTO stats = voteStatisticsService.calculateStatistics(votes);

        // Then
        assertEquals(4.2, stats.average(), 0.01);
        assertFalse(stats.isFullConsensus());
        assertEquals(1, stats.outlierVoteIds().size());
        assertEquals(outlierVote.getId(), stats.outlierVoteIds().get(0));
    }

    @Test
    @DisplayName("Debe retornar valores por defecto si la lista de votos está vacía")
    void shouldHandleEmptyList() {
        // Given
        List<Vote> votes = List.of();

        // When
        VoteStatisticsDTO stats = voteStatisticsService.calculateStatistics(votes);

        // Then
        assertEquals(0.0, stats.average());
        assertEquals(0.0, stats.consensusPercentage());
        assertFalse(stats.isFullConsensus());
        assertTrue(stats.outlierVoteIds().isEmpty());
    }

    @Test
    @DisplayName("La carta 0 cuenta en el promedio y los comodines (sin weight) se excluyen")
    void shouldCountZeroAndIgnoreWildcards() {
        // Given: 0, 2 y un comodín "?" (sin weight)
        Vote wildcard = new Vote();
        wildcard.setId(UUID.randomUUID());
        wildcard.setCardValue(new CardValue());

        List<Vote> votes = List.of(createVote(0), createVote(2), wildcard);

        // When
        VoteStatisticsDTO stats = voteStatisticsService.calculateStatistics(votes);

        // Then: el promedio se calcula solo con 0 y 2
        assertEquals(1.0, stats.average(), 0.001);
        assertEquals(50.0, stats.consensusPercentage(), 0.001);
        assertFalse(stats.isFullConsensus());
    }

    @Test
    @DisplayName("Si solo hay comodines no hay estadísticas")
    void shouldReturnDefaultsWhenOnlyWildcards() {
        Vote wildcard = new Vote();
        wildcard.setId(UUID.randomUUID());
        wildcard.setCardValue(new CardValue());

        VoteStatisticsDTO stats = voteStatisticsService.calculateStatistics(List.of(wildcard));

        assertEquals(0.0, stats.average());
        assertFalse(stats.isFullConsensus());
    }
}
