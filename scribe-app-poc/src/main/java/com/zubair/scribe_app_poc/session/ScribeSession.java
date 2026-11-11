package com.zubair.scribe_app_poc.session;

import com.zubair.scribe_app_poc.common.exception.InvalidSessionStateException;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "scribe_sessions")
public class ScribeSession {

    @Id
    private UUID id;

    @Column(nullable = false, length = 32)
    private String language;

    @Column(nullable = false)
    private boolean consent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SessionStatus status;

    @Lob
    @Column(nullable = false)
    private String transcript;

    @Lob
    @Column(name = "ai_draft")
    private String aiDraft;

    @Lob
    @Column(name = "approved_note")
    private String approvedNote;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ScribeSession() {
        // JPA requirement
    }

    public static ScribeSession createTranscribed(
            UUID id,
            String language,
            String transcript,
            boolean consent,
            Instant now
    ) {
        if (!consent) {
            throw new IllegalArgumentException("Practitioner consent is required to create a session");
        }
        if (transcript == null || transcript.isBlank()) {
            throw new IllegalArgumentException("Transcript must not be blank");
        }

        ScribeSession session = new ScribeSession();
        session.id = Objects.requireNonNull(id, "Session ID must not be null");
        session.language = Objects.requireNonNull(language, "Language must not be null");
        session.transcript = transcript.trim();
        session.consent = consent;
        session.status = SessionStatus.TRANSCRIBED;
        session.version = null; // null for new entity so JPA performs insert
        session.createdAt = Objects.requireNonNull(now, "CreatedAt must not be null");
        session.updatedAt = now;
        return session;
    }

    public void attachDraft(String draft, Instant now) {
        if (this.status == SessionStatus.APPROVED) {
            throw new InvalidSessionStateException("Cannot modify or attach draft to an already approved session");
        }
        if (draft == null || draft.isBlank()) {
            throw new IllegalArgumentException("AI draft must not be blank");
        }
        this.aiDraft = draft.trim();
        this.status = SessionStatus.DRAFTED;
        this.updatedAt = Objects.requireNonNull(now, "UpdatedAt must not be null");
    }

    public void approve(String finalNote, Instant now) {
        if (this.status == SessionStatus.APPROVED) {
            throw new InvalidSessionStateException("Session has already been approved");
        }
        if (this.status != SessionStatus.DRAFTED) {
            throw new InvalidSessionStateException("Session must be in DRAFTED status before it can be approved");
        }
        if (finalNote == null || finalNote.isBlank()) {
            throw new IllegalArgumentException("Approved note must not be blank");
        }
        this.approvedNote = finalNote.trim();
        this.status = SessionStatus.APPROVED;
        this.updatedAt = Objects.requireNonNull(now, "UpdatedAt must not be null");
    }

    public UUID getId() {
        return id;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isConsent() {
        return consent;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public String getTranscript() {
        return transcript;
    }

    public String getAiDraft() {
        return aiDraft;
    }

    public String getApprovedNote() {
        return approvedNote;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

