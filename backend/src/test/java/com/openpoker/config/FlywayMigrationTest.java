package com.openpoker.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El esquema se crea solo con migraciones. Si este test y el arranque del contexto pasan,
 * las migraciones corren en limpio y coinciden con las entidades (ddl-auto=validate).
 */
@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("Todas las migraciones están aplicadas y no queda ninguna pendiente")
    void allMigrationsApplied() {
        var info = flyway.info();

        assertNotNull(info.current(), "Flyway no aplicó ninguna migración");
        assertEquals(0, info.pending().length, "Hay migraciones pendientes");
        assertTrue(info.applied().length >= 2, "Se esperaban al menos V1 (baseline) y V2 (índices)");
    }
}
