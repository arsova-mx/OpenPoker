package com.openpoker.service;

import org.springframework.stereotype.Service;

/**
 * Temporary hello-world service.
 *
 * <p>This will be replaced by the actual OpenPoker services:
 * <ul>
 *   <li>{@code SessionService}  – business logic for planning poker sessions</li>
 *   <li>{@code StoryService}    – user story lifecycle management</li>
 *   <li>{@code VoteService}     – vote submission, reveal and scoring</li>
 * </ul>
 */
@Service
public class HelloService {

    public String greet() {
        return "Hello from OpenPoker! 🃏";
    }
}
