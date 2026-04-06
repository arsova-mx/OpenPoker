package com.openpoker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OpenPoker – Planning Poker application entry point.
 *
 * <p>Future features to implement in this Spring Boot application:
 * <ul>
 *   <li>Session management (create/join planning poker rooms)</li>
 *   <li>Real-time voting via WebSocket (STOMP)</li>
 *   <li>Story management within a session</li>
 *   <li>Vote reveal and consensus tracking</li>
 * </ul>
 */
@SpringBootApplication
public class OpenPokerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenPokerApplication.class, args);
    }
}
