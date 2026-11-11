package com.zubair.scribe_app_poc.session.dto;

import com.zubair.scribe_app_poc.session.SessionStatus;

import java.time.Instant;
import java.util.UUID;

public record SessionSummary(
        UUID id,
        Instant createdAt,
        String language,
        SessionStatus status
) {
}

