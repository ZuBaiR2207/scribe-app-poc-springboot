package com.zubair.scribe_app_poc.session.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
        @NotBlank(message = "Language is required")
        String language,

        @NotBlank(message = "Transcript must not be blank")
        @Size(max = 50000, message = "Transcript exceeds maximum supported length")
        String transcript,

        @NotNull(message = "Consent flag is required")
        @AssertTrue(message = "Practitioner consent must be accepted")
        Boolean consent
) {
}

