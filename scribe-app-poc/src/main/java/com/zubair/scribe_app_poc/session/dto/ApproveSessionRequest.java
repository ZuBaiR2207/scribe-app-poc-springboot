package com.zubair.scribe_app_poc.session.dto;

import jakarta.validation.constraints.NotBlank;

public record ApproveSessionRequest(
        @NotBlank(message = "Approved note must not be blank")
        String approvedNote
) {
}

