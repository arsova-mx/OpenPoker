package com.openpoker.controller;

import com.openpoker.service.HelloService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Temporary hello-world controller.
 *
 * <p>This will be replaced by the actual OpenPoker controllers:
 * <ul>
 *   <li>{@code SessionController}  – create/join/close poker sessions</li>
 *   <li>{@code StoryController}    – manage user stories inside a session</li>
 *   <li>{@code VoteController}     – submit and reveal votes</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class HelloController {

    private final HelloService helloService;

    public HelloController(HelloService helloService) {
        this.helloService = helloService;
    }

    /** GET /api/hello – sanity-check endpoint. */
    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("message", helloService.greet());
    }
}
