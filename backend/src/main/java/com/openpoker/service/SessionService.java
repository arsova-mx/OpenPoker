package com.openpoker.service;

import com.openpoker.entity.Session;
import com.openpoker.entity.SessionParticipant;
import com.openpoker.globalexception.ParticipantNotFoundException;
import com.openpoker.globalexception.SessionNotFoundException;
import com.openpoker.repository.SessionParticipantRepository;
import com.openpoker.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionRepository sessionRepository;
    private final SessionParticipantRepository sessionParticipantRepository;

    public Session createSession(String name) {
        Session session = new Session();
        session.setName(name);
        session.setInviteCode(generateCode());
        session.setStatus(Session.Status.WAITING);

        return sessionRepository.save(session);
    }

    public SessionParticipant joinSession(String inviteCode, String displayName) {
        Session session = sessionRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new SessionNotFoundException("session not found"));

        SessionParticipant participant = new SessionParticipant();
        participant.setDisplayName(displayName);
        participant.setSession(session);

        return sessionParticipantRepository.save(participant);
    }

    public List<SessionParticipant> getParticipants(Session session) {
        return sessionParticipantRepository.findBySession(session);
    }

    private String generateCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public Session leaveSession(UUID participantId) {
        SessionParticipant participant = sessionParticipantRepository.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException("Participante no encontrado"));

        Session session = participant.getSession();
        sessionParticipantRepository.delete(participant);
        return session;
    }
}
