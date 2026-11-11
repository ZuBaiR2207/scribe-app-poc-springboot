package com.zubair.scribe_app_poc.session.dto;

import com.zubair.scribe_app_poc.session.SessionStatus;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        String language,
        boolean consent,
        SessionStatus status,
        String transcript,
        String aiDraft,
        String approvedNote,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}

