package com.openpoker.controller;

import com.openpoker.dto.CastVoteRequest;
import com.openpoker.dto.VoteResponse;
import com.openpoker.dto.VotingResultsResponse;
import com.openpoker.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions/{code}/votes")
@RequiredArgsConstructor
public class VoteController {
    private final VoteService service;

    @PostMapping
    public ResponseEntity<VoteResponse> vote(@AuthenticationPrincipal String username, @PathVariable String code, @RequestBody @Valid CastVoteRequest request) {
        return ResponseEntity.ok(service.castVote(username, code, request));
    }

    @GetMapping
    public ResponseEntity<VotingResultsResponse> getVotes(@PathVariable String code) {
        return ResponseEntity.ok(service.getVotes(code));
    }

    @PostMapping("/reveal")
    public ResponseEntity<VotingResultsResponse> reveal(@AuthenticationPrincipal String username, @PathVariable
    String code) {
        return ResponseEntity.ok(service.revealVotes(username, code));
    }
}
