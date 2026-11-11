package com.zubair.scribe_app_poc.session;

import com.zubair.scribe_app_poc.NoteGenerator;
import com.zubair.scribe_app_poc.common.exception.InvalidSessionStateException;
import com.zubair.scribe_app_poc.common.exception.NoteGenerationException;
import com.zubair.scribe_app_poc.common.exception.SessionNotFoundException;
import com.zubair.scribe_app_poc.config.ScribeProperties;
import com.zubair.scribe_app_poc.session.dto.ApproveSessionRequest;
import com.zubair.scribe_app_poc.session.dto.CreateSessionRequest;
import com.zubair.scribe_app_poc.session.dto.SessionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private NoteGenerator noteGenerator;

    private ScribeProperties properties;
    private Clock fixedClock;
    private SessionServiceImpl sessionService;

    @BeforeEach
    void setUp() {
        properties = new ScribeProperties(
                new ScribeProperties.Limits(50000),
                List.of("en", "cmn_en", "ms", "en_ms")
        );
        fixedClock = Clock.fixed(Instant.parse("2026-11-11T12:00:00Z"), ZoneOffset.UTC);
        sessionService = new SessionServiceImpl(
                sessionRepository,
                noteGenerator,
                properties,
                fixedClock
        );
    }

    @Test
    @DisplayName("createSession rejects missing consent")
    void createSession_rejectsMissingConsent() {
        CreateSessionRequest request = new CreateSessionRequest("en", "Discussed nutrition.", false);

        assertThatThrownBy(() -> sessionService.createSession(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consent");

        verifyNoInteractions(sessionRepository);
        verifyNoInteractions(noteGenerator);
    }

    @Test
    @DisplayName("Successful create stores TRANSCRIBED then DRAFTED")
    void createSession_storesTranscribedThenDrafted() {
        CreateSessionRequest request = new CreateSessionRequest("en", "Discussed hydration and stress.", true);

        when(sessionRepository.saveAndFlush(any(ScribeSession.class))).thenAnswer(inv -> {
            ScribeSession s = inv.getArgument(0);
            assertThat(s.getStatus()).isEqualTo(SessionStatus.TRANSCRIBED);
            return s;
        });
        when(sessionRepository.save(any(ScribeSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(noteGenerator.generate(eq("Discussed hydration and stress.")))
                .thenReturn("Session summary: Discussed hydration and stress.");

        SessionResponse response = sessionService.createSession(request);

        assertThat(response.status()).isEqualTo(SessionStatus.DRAFTED);
        assertThat(response.aiDraft()).isEqualTo("Session summary: Discussed hydration and stress.");
        assertThat(response.approvedNote()).isNull();

        verify(sessionRepository).saveAndFlush(any(ScribeSession.class));
        verify(sessionRepository).save(any(ScribeSession.class));
    }

    @Test
    @DisplayName("LLM failure leaves the session TRANSCRIBED and throws NoteGenerationException")
    void createSession_llmFailure_leavesTranscribedAndThrows() {
        CreateSessionRequest request = new CreateSessionRequest("en", "Discussed knee pain.", true);

        when(sessionRepository.saveAndFlush(any(ScribeSession.class))).thenAnswer(inv -> {
            ScribeSession s = inv.getArgument(0);
            assertThat(s.getStatus()).isEqualTo(SessionStatus.TRANSCRIBED);
            return s;
        });
        when(noteGenerator.generate(any()))
                .thenThrow(new RuntimeException("LLM API unavailable"));

        assertThatThrownBy(() -> sessionService.createSession(request))
                .isInstanceOf(NoteGenerationException.class)
                .hasMessageContaining("Failed to generate note");

        verify(sessionRepository).saveAndFlush(any(ScribeSession.class));
        verify(sessionRepository, never()).save(argThat(s -> s.getStatus() == SessionStatus.DRAFTED));
    }

    @Test
    @DisplayName("Approve works from DRAFTED state")
    void approveSession_worksFromDrafted() {
        UUID id = UUID.randomUUID();
        ScribeSession session = ScribeSession.createTranscribed(id, "en", "Transcript", true, fixedClock.instant());
        session.attachDraft("AI draft notes", fixedClock.instant());

        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ScribeSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse response = sessionService.approveSession(id, new ApproveSessionRequest("Final practitioner note"));

        assertThat(response.status()).isEqualTo(SessionStatus.APPROVED);
        assertThat(response.approvedNote()).isEqualTo("Final practitioner note");
        assertThat(response.aiDraft()).isEqualTo("AI draft notes");
    }

    @Test
    @DisplayName("Approving twice throws InvalidSessionStateException (409)")
    void approveSession_approvingTwice_throws() {
        UUID id = UUID.randomUUID();
        ScribeSession session = ScribeSession.createTranscribed(id, "en", "Transcript", true, fixedClock.instant());
        session.attachDraft("AI draft notes", fixedClock.instant());
        session.approve("Initial approval", fixedClock.instant());

        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.approveSession(id, new ApproveSessionRequest("Second approval")))
                .isInstanceOf(InvalidSessionStateException.class)
                .hasMessageContaining("already been approved");
    }

    @Test
    @DisplayName("Approving a blank note throws IllegalArgumentException")
    void approveSession_blankNote_throws() {
        UUID id = UUID.randomUUID();
        ScribeSession session = ScribeSession.createTranscribed(id, "en", "Transcript", true, fixedClock.instant());
        session.attachDraft("AI draft notes", fixedClock.instant());

        when(sessionRepository.findById(id)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.approveSession(id, new ApproveSessionRequest("   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("Delete of unknown id throws SessionNotFoundException")
    void deleteSession_unknownId_throws() {
        UUID id = UUID.randomUUID();
        when(sessionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.deleteSession(id))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining("not found");
    }
}

