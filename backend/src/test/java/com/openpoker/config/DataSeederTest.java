package com.openpoker.config;

import com.openpoker.entity.CardValue;
import com.openpoker.entity.VotingDeck;
import com.openpoker.model.CardSeries;
import com.openpoker.repository.VotingDeckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El seeder no corre en el perfil "test" (@Profile("!test")), así que se instancia a mano
 * contra la BD H2 de pruebas.
 */
@SpringBootTest
@ActiveProfiles("test")
class DataSeederTest {

    @Autowired
    private VotingDeckRepository deckRepository;

    private DataSeeder seeder;

    @BeforeEach
    void setUp() {
        deckRepository.deleteAll();
        seeder = new DataSeeder(deckRepository);
    }

    @Test
    @DisplayName("Con la BD vacía crea las 3 barajas con sus cartas, orden y pesos")
    void seedsAllDecksWithCardsAndWeights() {
        seeder.run();

        assertEquals(3, deckRepository.count());

        VotingDeck fibonacci = loadDeck(CardSeries.FIBONACCI);
        assertEquals(
                List.of("0", "1", "2", "3", "5", "8", "13", "21", "34", "55", "89", "?", "☕"),
                values(fibonacci));
        assertEquals(0, weightOf(fibonacci, "0"));
        assertEquals(13, weightOf(fibonacci, "13"));
        assertNull(weightOf(fibonacci, "?"));
        assertNull(weightOf(fibonacci, "☕"));

        VotingDeck tShirt = loadDeck(CardSeries.T_SHIRT);
        assertEquals(List.of("XS", "S", "M", "L", "XL", "XXL", "?", "☕"), values(tShirt));
        assertEquals(8, weightOf(tShirt, "XL"));

        VotingDeck dots = loadDeck(CardSeries.DOT_VOTING);
        assertEquals(List.of("1", "2", "3", "4", "5"), values(dots));
        assertEquals(5, weightOf(dots, "5"));
    }

    @Test
    @DisplayName("Ejecutarlo varias veces no duplica barajas ni cartas")
    void isIdempotent() {
        seeder.run();
        seeder.run();

        assertEquals(3, deckRepository.count());
        assertEquals(13, loadDeck(CardSeries.FIBONACCI).getCardValues().size());
    }

    @Test
    @DisplayName("Repara una baraja existente que quedó sin cartas")
    void repairsDeckWithoutCards() {
        deckRepository.save(VotingDeck.builder()
                .name("Fibonacci")
                .seriesType(CardSeries.FIBONACCI)
                .description("Baraja sembrada sin cartas")
                .cardValues(new ArrayList<>())
                .build());

        seeder.run();

        assertEquals(3, deckRepository.count());
        assertEquals(13, loadDeck(CardSeries.FIBONACCI).getCardValues().size());
    }

    @Test
    @DisplayName("Completa el weight de cartas existentes que no lo tienen")
    void backfillsMissingWeights() {
        VotingDeck deck = VotingDeck.builder()
                .name("Dot Voting")
                .seriesType(CardSeries.DOT_VOTING)
                .description("Baraja sin pesos")
                .cardValues(new ArrayList<>())
                .build();
        for (int i = 1; i <= 5; i++) {
            deck.getCardValues().add(CardValue.builder()
                    .value(String.valueOf(i))
                    .orderIndex(i - 1)
                    .deck(deck)
                    .build());
        }
        deckRepository.save(deck);

        seeder.run();

        VotingDeck repaired = loadDeck(CardSeries.DOT_VOTING);
        assertEquals(5, repaired.getCardValues().size());
        assertEquals(Map.of("1", 1, "2", 2, "3", 3, "4", 4, "5", 5), weights(repaired));
    }

    private VotingDeck loadDeck(CardSeries seriesType) {
        return deckRepository.findBySeriesTypeWithCards(seriesType)
                .orElseThrow(() -> new AssertionError("No existe la baraja " + seriesType));
    }

    private static List<String> values(VotingDeck deck) {
        return deck.getCardValues().stream().map(CardValue::getValue).toList();
    }

    private static Integer weightOf(VotingDeck deck, String value) {
        return deck.getCardValues().stream()
                .filter(card -> card.getValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No existe la carta " + value))
                .getWeight();
    }

    private static Map<String, Integer> weights(VotingDeck deck) {
        Map<String, Integer> weights = new java.util.HashMap<>();
        deck.getCardValues().forEach(card -> weights.put(card.getValue(), card.getWeight()));
        return weights;
    }
}
