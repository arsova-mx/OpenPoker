package com.openpoker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.flyway.enabled=false")
@ActiveProfiles("test")
class OpenPokerApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring application context starts without errors.
    }
}
