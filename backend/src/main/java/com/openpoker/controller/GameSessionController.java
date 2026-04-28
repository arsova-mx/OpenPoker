package com.openpoker.controller;

import com.openpoker.dto.CreateSessionRequest;
import com.openpoker.dto.SessionResponse;
import com.openpoker.service.GameSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class GameSessionController {
    private final GameSessionService gameSessionService;

    @PostMapping
    public ResponseEntity<SessionResponse> create(@AuthenticationPrincipal String username, @RequestBody
    @Valid CreateSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gameSessionService.createSession(username, request));
    }

    @GetMapping("/{code}")
    public ResponseEntity<SessionResponse> get(@PathVariable String code) {
        return ResponseEntity.ok(gameSessionService.getSessionByCode(code));
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<SessionResponse> join(@AuthenticationPrincipal String username, @PathVariable String code) {
        return ResponseEntity.ok(gameSessionService.joinSession(username, code));
    }

}

