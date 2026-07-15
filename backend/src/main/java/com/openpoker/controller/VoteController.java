package com.openpoker.controller;

import com.openpoker.dto.CastVoteRequest;
import com.openpoker.dto.VoteResponse;
import com.openpoker.dto.VotingRRAverage;
import com.openpoker.dto.VotingResultsResponse;
import com.openpoker.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions/{code}/votes")
@RequiredArgsConstructor
public class VoteController {
    private final VoteService service;

    @PostMapping
    public ResponseEntity<VoteResponse> vote(@AuthenticationPrincipal String username, @PathVariable String code,@RequestParam UUID ticketId, @RequestBody @Valid CastVoteRequest request) {
        return ResponseEntity.ok(service.castVote(username, code,ticketId, request));
    }

    @GetMapping
    public ResponseEntity<VotingResultsResponse> getVotes(@AuthenticationPrincipal String username, @PathVariable String code,@RequestParam UUID ticketId) {
        return ResponseEntity.ok(service.getVotes(code,ticketId, username));
    }

    @PostMapping("/reveal")
    public ResponseEntity<VotingRRAverage> reveal(@AuthenticationPrincipal String username, @PathVariable String code,@RequestParam UUID ticketId) {
        return ResponseEntity.ok(service.revealVotes(username, code,ticketId));
    }
}
