package com.openpoker.controller;

import com.openpoker.dto.CreateSessionRequest;
import com.openpoker.dto.SessionResponse;
import com.openpoker.dto.VotingDeckResponse;
import com.openpoker.service.CardDeckService;
import com.openpoker.service.GameSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class GameSessionController {
    private final GameSessionService gameSessionService;
    private final CardDeckService cardDeckService;

    @PostMapping
    public ResponseEntity<SessionResponse> createWithDefaultDeck(@AuthenticationPrincipal String username, @RequestBody
    @Valid CreateSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gameSessionService.createSessionWithDefaultDeck(username, request));
    }

    @PostMapping("/{deckId}")
    public ResponseEntity<SessionResponse> createWithDeck(@AuthenticationPrincipal String username, @PathVariable UUID deckId, @RequestBody
    @Valid CreateSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gameSessionService.createSession(username, request, deckId));
    }

    @GetMapping("/{code}")
    public ResponseEntity<SessionResponse> get(@PathVariable String code) {
        return ResponseEntity.ok(gameSessionService.getSessionByCode(code));
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<SessionResponse> join(@AuthenticationPrincipal String username, @PathVariable String code) {
        return ResponseEntity.ok(gameSessionService.joinSession(username, code));
    }

    @GetMapping
    public List<VotingDeckResponse> getAllDecks() {
        return cardDeckService.getAllDecks();
    }

    @PostMapping("/{code}/finish")
    public ResponseEntity<SessionResponse> finish(@AuthenticationPrincipal String username, @PathVariable String code){
        return ResponseEntity.ok(gameSessionService.finishSession(username, code));
    }
}

