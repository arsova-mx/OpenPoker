package com.openpoker.controller;

import com.openpoker.entity.Session;
import com.openpoker.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions")
public class SessionController {
    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, String> req) {
        Session s = sessionService.createSession(req.get("name"));

        return ResponseEntity.ok().body(Map.of("sessionId,", s.getId(), "inviteCode", s.getInviteCode()));
    }
}
