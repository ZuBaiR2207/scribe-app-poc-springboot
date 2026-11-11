package com.zubair.scribe_app_poc.session;

import com.zubair.scribe_app_poc.session.dto.ApproveSessionRequest;
import com.zubair.scribe_app_poc.session.dto.CreateSessionRequest;
import com.zubair.scribe_app_poc.session.dto.SessionResponse;
import com.zubair.scribe_app_poc.session.dto.SessionSummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        SessionResponse response = sessionService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SessionSummary>> listSessions() {
        return ResponseEntity.ok(sessionService.listSessions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSession(@PathVariable UUID id) {
        return ResponseEntity.ok(sessionService.getSession(id));
    }

    @PostMapping("/{id}/draft")
    public ResponseEntity<SessionResponse> regenerateDraft(@PathVariable UUID id) {
        return ResponseEntity.ok(sessionService.regenerateDraft(id));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<SessionResponse> approveSession(
            @PathVariable UUID id,
            @Valid @RequestBody ApproveSessionRequest request
    ) {
        return ResponseEntity.ok(sessionService.approveSession(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}

