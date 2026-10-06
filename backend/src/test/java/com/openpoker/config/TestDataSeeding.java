package com.openpoker.config;

import com.openpoker.repository.VotingDeckRepository;
import org.springframework.stereotype.Component;

/**
 * En el perfil "test" el DataSeeder no corre automáticamente; los tests de integración
 * que necesitan barajas las siembran con este helper (idempotente).
 */
@Component
public class TestDataSeeding {
    private final VotingDeckRepository deckRepository;

    public TestDataSeeding(VotingDeckRepository deckRepository) {
        this.deckRepository = deckRepository;
    }

    public void seedDecks() {
        new DataSeeder(deckRepository).run();
    }
}
