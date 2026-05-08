package com.openpoker.controller;

import com.openpoker.dto.CreateSessionRequest;
import com.openpoker.dto.SessionResponse;
import com.openpoker.entity.Session;
import com.openpoker.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/session-management")
public class SessionController {
    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, String> req) {
        Session s = sessionService.createSession(req.get("name"));

        return ResponseEntity.ok().body(Map.of("sessionId", s.getId(), "inviteCode", s.getInviteCode()));
    }
}
