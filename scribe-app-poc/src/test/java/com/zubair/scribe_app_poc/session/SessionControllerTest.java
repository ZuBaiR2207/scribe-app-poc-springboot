package com.zubair.scribe_app_poc.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zubair.scribe_app_poc.common.GlobalExceptionHandler;
import com.zubair.scribe_app_poc.common.exception.InvalidSessionStateException;
import com.zubair.scribe_app_poc.common.exception.SessionNotFoundException;
import com.zubair.scribe_app_poc.session.dto.ApproveSessionRequest;
import com.zubair.scribe_app_poc.session.dto.CreateSessionRequest;
import com.zubair.scribe_app_poc.session.dto.SessionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SessionController.class)
@Import(GlobalExceptionHandler.class)
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SessionService sessionService;

    @Test
    @DisplayName("Validation failure returns ProblemDetail with 400 Bad Request")
    void createSession_validationFailure_returns400() throws Exception {
        CreateSessionRequest invalidRequest = new CreateSessionRequest("en", "Valid transcript", false);

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.consent").exists());
    }

    @Test
    @DisplayName("Unknown session ID returns ProblemDetail with 404 Not Found")
    void getSession_unknownId_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(sessionService.getSession(id)).thenThrow(new SessionNotFoundException("Session not found: " + id));

        mockMvc.perform(get("/api/sessions/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.title").value("Session Not Found"))
                .andExpect(jsonPath("$.detail").value("Session not found: " + id));
    }

    @Test
    @DisplayName("Double approve returns ProblemDetail with 409 Conflict")
    void approveSession_doubleApprove_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        ApproveSessionRequest request = new ApproveSessionRequest("Approved note");

        when(sessionService.approveSession(eq(id), any(ApproveSessionRequest.class)))
                .thenThrow(new InvalidSessionStateException("Session has already been approved"));

        mockMvc.perform(put("/api/sessions/{id}/approve", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.title").value("Invalid Session State"))
                .andExpect(jsonPath("$.detail").value("Session has already been approved"));
    }

    @Test
    @DisplayName("Valid createSession returns 201 Created")
    void createSession_valid_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        CreateSessionRequest request = new CreateSessionRequest("en", "Patient notes", true);

        SessionResponse mockResponse = new SessionResponse(
                id,
                "en",
                true,
                SessionStatus.DRAFTED,
                "Patient notes",
                "AI Draft notes",
                null,
                0L,
                Instant.now(),
                Instant.now()
        );

        when(sessionService.createSession(any(CreateSessionRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("DRAFTED"))
                .andExpect(jsonPath("$.aiDraft").value("AI Draft notes"));
    }
}

