package com.zubair.scribe_app_poc;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
public class NoteController {

    private final NoteGenerator generator;

    public NoteController(NoteGenerator generator) {
        this.generator = generator;
    }

    public record NoteRequest(String transcript) {
    }

    @PostMapping("/api/notes")
    public Map<String, String> create(@RequestBody NoteRequest req) {
        if (req.transcript() == null || req.transcript().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transcript is empty");
        }
        try {
            return Map.of("note", generator.generate(req.transcript()));
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "LLM provider temporarily unavailable: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate note: " + e.getMessage());
        }
    }
}