package com.openpoker.controller;

import com.openpoker.dto.CreateSessionRequest;
import com.openpoker.dto.GuestJoinRequest;
import com.openpoker.dto.GuestJoinResponse;
import com.openpoker.dto.JoinSessionRequest;
import com.openpoker.dto.SessionResponse;
import com.openpoker.dto.VotingDeckResponse;
import com.openpoker.entity.Participant;
import com.openpoker.security.JwtService;
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
    private final JwtService jwtService;

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

    /** Une al usuario autenticado a la sala (idempotente: si ya participa, lo reutiliza). */
    @PostMapping("/{code}/join")
    public ResponseEntity<SessionResponse> join(@AuthenticationPrincipal String username, @PathVariable String code) {
        return ResponseEntity.ok(gameSessionService.joinSession(new JoinSessionRequest(code, username, null)));
    }

    /**
     * Une a un invitado sin cuenta. Devuelve un token de invitado que se usa en el CONNECT de STOMP;
     * no sirve para la API REST ni para otras salas.
     */
    @PostMapping("/{code}/guests")
    public ResponseEntity<GuestJoinResponse> joinAsGuest(@PathVariable String code, @RequestBody @Valid GuestJoinRequest request) {
        SessionResponse sessionResp = gameSessionService.joinSession(new JoinSessionRequest(code, null, request.guestName()));
        
        // Buscamos al participante recién creado o reutilizado
        Participant guest = gameSessionService.getParticipants(code).stream()
                .filter(p -> request.guestName().equals(p.getGuestDisplayName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se pudo registrar al invitado"));

        String guestToken = jwtService.generateGuestToken(guest.getId(), code);

        return ResponseEntity.status(HttpStatus.CREATED).body(new GuestJoinResponse(
                sessionResp,
                guest.getId(),
                guest.getGuestDisplayName(),
                guestToken));
    }

    @GetMapping
    public List<VotingDeckResponse> getAllDecks() {
        return cardDeckService.getAllDecks();
    }
}