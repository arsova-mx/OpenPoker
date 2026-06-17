package com.openpoker.config;

import com.openpoker.entity.CardValue;
import com.openpoker.entity.VotingDeck;
import com.openpoker.model.CardSeries;
import com.openpoker.repository.VotingDeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataSeeder implements CommandLineRunner {
    private final VotingDeckRepository deckRepository;

    @Override
    public void run(String... args) {
        if (deckRepository.count() > 0) {
            return;
        }

        createDeck(
                "Fibonacci",
                CardSeries.FIBONACCI,
                "Estimación relativa clásica",
                List.of("0", "1", "2", "3", "5", "8", "13", "21", "34", "55", "89", "?", "☕")
        );

        createDeck(
                "T-Shirt Sizes",
                CardSeries.T_SHIRT,
                "Estimación rápida sin números",
                List.of("XS", "S", "M", "L", "XL", "XXL", "?", "☕")
        );

        createDeck(
                "Dot Voting",
                CardSeries.DOT_VOTING,
                "Votación simple de priorización",
                List.of("1", "2", "3", "4", "5")
        );
    }

    private void createDeck(String name, CardSeries seriesType, String description, List<String> values) {
        VotingDeck deck = VotingDeck.builder()
                .name(name)
                .seriesType(seriesType)
                .description(description)
                .values(new ArrayList<>())
                .build();

        List<CardValue> CardValues = values.stream()
                .map(v -> CardValue.builder().value(v).deck(deck).build())
                .toList();

        deck.setValues(CardValues);
        deckRepository.save(deck);
    }
}
