package com.zubair.scribe_app_poc.session;

import com.zubair.scribe_app_poc.session.dto.SessionResponse;
import com.zubair.scribe_app_poc.session.dto.SessionSummary;

public final class SessionMapper {

    private SessionMapper() {
    }

    public static SessionResponse toResponse(ScribeSession session) {
        return new SessionResponse(
                session.getId(),
                session.getLanguage(),
                session.isConsent(),
                session.getStatus(),
                session.getTranscript(),
                session.getAiDraft(),
                session.getApprovedNote(),
                session.getVersion(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    public static SessionSummary toSummary(ScribeSession session) {
        return new SessionSummary(
                session.getId(),
                session.getCreatedAt(),
                session.getLanguage(),
                session.getStatus()
        );
    }
}

