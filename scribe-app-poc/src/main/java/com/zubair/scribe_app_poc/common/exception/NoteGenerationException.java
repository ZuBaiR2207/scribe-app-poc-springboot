package com.zubair.scribe_app_poc.common.exception;

import java.util.UUID;

public class NoteGenerationException extends RuntimeException {

    private final UUID sessionId;

    public NoteGenerationException(String message, UUID sessionId) {
        super(message);
        this.sessionId = sessionId;
    }

    public NoteGenerationException(String message, UUID sessionId, Throwable cause) {
        super(message, cause);
        this.sessionId = sessionId;
    }

    public UUID getSessionId() {
        return sessionId;
    }
}

