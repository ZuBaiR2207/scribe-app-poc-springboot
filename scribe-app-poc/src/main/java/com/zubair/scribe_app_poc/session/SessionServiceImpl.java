package com.zubair.scribe_app_poc.session;

import com.zubair.scribe_app_poc.NoteGenerator;
import com.zubair.scribe_app_poc.common.exception.InvalidSessionStateException;
import com.zubair.scribe_app_poc.common.exception.NoteGenerationException;
import com.zubair.scribe_app_poc.common.exception.SessionNotFoundException;
import com.zubair.scribe_app_poc.config.ScribeProperties;
import com.zubair.scribe_app_poc.session.dto.ApproveSessionRequest;
import com.zubair.scribe_app_poc.session.dto.CreateSessionRequest;
import com.zubair.scribe_app_poc.session.dto.SessionResponse;
import com.zubair.scribe_app_poc.session.dto.SessionSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SessionServiceImpl implements SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionServiceImpl.class);

    private final SessionRepository sessionRepository;
    private final NoteGenerator noteGenerator;
    private final ScribeProperties properties;
    private final Clock clock;

    public SessionServiceImpl(
            SessionRepository sessionRepository,
            NoteGenerator noteGenerator,
            ScribeProperties properties,
            Clock clock
    ) {
        this.sessionRepository = sessionRepository;
        this.noteGenerator = noteGenerator;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public SessionResponse createSession(CreateSessionRequest request) {
        if (request.consent() == null || !request.consent()) {
            throw new IllegalArgumentException("Practitioner consent is mandatory");
        }

        List<String> allowedLanguages = properties.allowedLanguages();
        if (allowedLanguages != null && !allowedLanguages.contains(request.language())) {
            throw new IllegalArgumentException("Unsupported language: " + request.language());
        }

        int maxLength = properties.limits() != null ? properties.limits().maxTranscriptLength() : 50000;
        if (request.transcript() != null && request.transcript().length() > maxLength) {
            throw new IllegalArgumentException("Transcript exceeds maximum allowed length of " + maxLength);
        }

        UUID sessionId = UUID.randomUUID();
        Instant now = clock.instant();

        // 1. Persist as TRANSCRIBED before calling the LLM
        ScribeSession session = ScribeSession.createTranscribed(
                sessionId,
                request.language(),
                request.transcript(),
                request.consent(),
                now
        );
        session = sessionRepository.saveAndFlush(session);
        log.info("Persisted session: id={}, status={}", sessionId, session.getStatus());

        // 2. Call NoteGenerator to produce AI draft
        try {
            String draft = noteGenerator.generate(session.getTranscript());
            session.attachDraft(draft, clock.instant());
            session = sessionRepository.save(session);
            log.info("Session drafted: id={}, status={}", sessionId, session.getStatus());
            return SessionMapper.toResponse(session);
        } catch (Exception ex) {
            log.warn("LLM generation failed for session id={}, remaining in TRANSCRIBED: {}", sessionId, ex.getMessage());
            // Session remains safely in TRANSCRIBED state in DB
            throw new NoteGenerationException("Failed to generate note from transcript: " + ex.getMessage(), sessionId, ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SessionResponse getSession(UUID id) {
        ScribeSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + id));
        return SessionMapper.toResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionSummary> listSessions() {
        return sessionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(SessionMapper::toSummary)
                .toList();
    }

    @Override
    public SessionResponse regenerateDraft(UUID id) {
        ScribeSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + id));

        if (session.getStatus() == SessionStatus.APPROVED) {
            throw new InvalidSessionStateException("Cannot modify or regenerate draft for an already approved session");
        }

        try {
            String draft = noteGenerator.generate(session.getTranscript());
            session.attachDraft(draft, clock.instant());
            session = sessionRepository.save(session);
            log.info("Session draft regenerated: id={}, status={}", id, session.getStatus());
            return SessionMapper.toResponse(session);
        } catch (Exception ex) {
            log.warn("LLM draft regeneration failed for session id={}: {}", id, ex.getMessage());
            throw new NoteGenerationException("Failed to regenerate note: " + ex.getMessage(), id, ex);
        }
    }

    @Override
    public SessionResponse approveSession(UUID id, ApproveSessionRequest request) {
        ScribeSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + id));

        if (session.getStatus() == SessionStatus.APPROVED) {
            throw new InvalidSessionStateException("Session has already been approved");
        }
        if (session.getStatus() != SessionStatus.DRAFTED) {
            throw new InvalidSessionStateException("Session must be in DRAFTED status before it can be approved");
        }

        session.approve(request.approvedNote(), clock.instant());
        session = sessionRepository.save(session);
        log.info("Session approved: id={}, status={}", id, session.getStatus());

        return SessionMapper.toResponse(session);
    }

    @Override
    public void deleteSession(UUID id) {
        ScribeSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + id));
        sessionRepository.delete(session);
        log.info("Session deleted: id={}", id);
    }
}

