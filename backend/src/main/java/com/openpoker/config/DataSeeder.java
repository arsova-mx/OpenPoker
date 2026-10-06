package com.openpoker.config;

import com.openpoker.entity.CardValue;
import com.openpoker.entity.VotingDeck;
import com.openpoker.model.CardSeries;
import com.openpoker.repository.VotingDeckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Siembra las barajas de estimación de forma idempotente:
 * - Crea las barajas que no existan.
 * - Repara barajas existentes sin cartas (instalaciones sembradas cuando el seeder no persistía las cartas).
 * - Completa el {@code weight} de las cartas que no lo tengan; lo usan las estadísticas y la carta sugerida.
 *   Los comodines ("?" y "☕") no tienen peso y quedan fuera de las estadísticas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataSeeder implements CommandLineRunner {
    static final List<DeckDefinition> DEFAULT_DECKS = List.of(
            new DeckDefinition(
                    "Fibonacci",
                    CardSeries.FIBONACCI,
                    "Estimación relativa clásica",
                    cards("0", 0, "1", 1, "2", 2, "3", 3, "5", 5, "8", 8, "13", 13, "21", 21,
                            "34", 34, "55", 55, "89", 89, "?", null, "☕", null)),
            new DeckDefinition(
                    "T-Shirt Sizes",
                    CardSeries.T_SHIRT,
                    "Estimación rápida sin números",
                    // Equivalencia aproximada a Fibonacci para poder calcular promedio y consenso
                    cards("XS", 1, "S", 2, "M", 3, "L", 5, "XL", 8, "XXL", 13, "?", null, "☕", null)),
            new DeckDefinition(
                    "Dot Voting",
                    CardSeries.DOT_VOTING,
                    "Votación simple de priorización",
                    cards("1", 1, "2", 2, "3", 3, "4", 4, "5", 5))
    );

    private final VotingDeckRepository deckRepository;

    @Override
    @Transactional
    public void run(String... args) {
        DEFAULT_DECKS.forEach(this::seedDeck);
    }

    void seedDeck(DeckDefinition definition) {
        Optional<VotingDeck> existing = deckRepository.findBySeriesTypeWithCards(definition.seriesType());

        if (existing.isEmpty()) {
            VotingDeck deck = VotingDeck.builder()
                    .name(definition.name())
                    .seriesType(definition.seriesType())
                    .description(definition.description())
                    .cardValues(new ArrayList<>())
                    .build();
            addCards(deck, definition);
            deckRepository.save(deck);
            log.info("Baraja '{}' creada con {} cartas", definition.name(), definition.cards().size());
            return;
        }

        VotingDeck deck = existing.get();
        if (deck.getCardValues() == null) {
            deck.setCardValues(new ArrayList<>());
        }

        if (deck.getCardValues().isEmpty()) {
            addCards(deck, definition);
            deckRepository.save(deck);
            log.info("Baraja '{}' reparada: se agregaron {} cartas", definition.name(), definition.cards().size());
            return;
        }

        boolean updated = false;
        for (CardValue card : deck.getCardValues()) {
            Integer expectedWeight = definition.cards().get(card.getValue());
            if (card.getWeight() == null && expectedWeight != null) {
                card.setWeight(expectedWeight);
                updated = true;
            }
        }
        if (updated) {
            deckRepository.save(deck);
            log.info("Baraja '{}': se completaron los pesos de las cartas", definition.name());
        }
    }

    private void addCards(VotingDeck deck, DeckDefinition definition) {
        int orderIndex = 0;
        for (Map.Entry<String, Integer> card : definition.cards().entrySet()) {
            deck.getCardValues().add(CardValue.builder()
                    .value(card.getKey())
                    .weight(card.getValue())
                    .orderIndex(orderIndex++)
                    .deck(deck)
                    .build());
        }
    }

    /** Cartas en orden de presentación, cada una con su peso numérico (null para comodines). */
    private static Map<String, Integer> cards(Object... valueWeightPairs) {
        Map<String, Integer> cards = new LinkedHashMap<>();
        for (int i = 0; i < valueWeightPairs.length; i += 2) {
            cards.put((String) valueWeightPairs[i], (Integer) valueWeightPairs[i + 1]);
        }
        return cards;
    }

    record DeckDefinition(String name, CardSeries seriesType, String description, Map<String, Integer> cards) {
    }
}
