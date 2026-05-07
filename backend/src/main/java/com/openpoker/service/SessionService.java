package com.openpoker.service;

import com.openpoker.entity.Participantt;
import com.openpoker.entity.Session;
import com.openpoker.globalexception.ParticipantNotFoundException;
import com.openpoker.globalexception.SessionNotFoundException;
import com.openpoker.repository.ParticipanttRepository;
import com.openpoker.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionRepository sessionRepository;
    private final ParticipanttRepository participanttRepository;

    public Session createSession(String name) {
        Session session = new Session();
        session.setName(name);
        session.setInviteCode(generateCode());
        session.setStatus(Session.Status.WAITING);

        return sessionRepository.save(session);
    }

    public Participantt joinSession(String inviteCode, String displayName) {
        Session session = sessionRepository.findByInviteCode(inviteCode).orElseThrow(()->new SessionNotFoundException(
                "session not found"));

        Participantt p = new Participantt();

        p.setDisplayName(displayName);
        p.setSession(session);

        return participanttRepository.save(p);
    }

    public List<Participantt> getParticipants(Session session) {
        return participanttRepository.findBySession(session);
    }

    private String generateCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public Session leaveSession(UUID participanttId) {
        Participantt participantt = participanttRepository.findById(participanttId).orElseThrow(()->new
                ParticipantNotFoundException("Participante no encontrado"));

        Session session = participantt.getSession();

        participanttRepository.delete(participantt);

        return session;
    }
}
