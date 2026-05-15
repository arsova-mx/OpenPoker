package com.openpoker.controller;

import com.openpoker.dto.VotingDeckResponse;
import com.openpoker.model.CardSeries;
import com.openpoker.service.CardDeckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/card-decks")
@RequiredArgsConstructor
public class CardDeckController {
    private final CardDeckService cardDeckService;

    @GetMapping
    public ResponseEntity<List<VotingDeckResponse>> getAllDecks() {
        return ResponseEntity.ok(cardDeckService.getAllDecks());
    }

    @GetMapping("/{seriesType}")
    public ResponseEntity<VotingDeckResponse> getDeckByType(@PathVariable CardSeries seriesType) {
        return ResponseEntity.ok(cardDeckService.getDeckBySeriesType(seriesType));
    }
}
