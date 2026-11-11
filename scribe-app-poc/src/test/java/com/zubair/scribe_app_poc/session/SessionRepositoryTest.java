package com.zubair.scribe_app_poc.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SessionRepositoryTest {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should save and find session by ID")
    void saveAndFind() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        ScribeSession session = ScribeSession.createTranscribed(
                id, "en", "Patient mentioned sore back.", true, now
        );

        sessionRepository.saveAndFlush(session);
        entityManager.clear();

        ScribeSession found = sessionRepository.findById(id).orElseThrow();
        assertThat(found.getId()).isEqualTo(id);
        assertThat(found.getLanguage()).isEqualTo("en");
        assertThat(found.getStatus()).isEqualTo(SessionStatus.TRANSCRIBED);
        assertThat(found.getTranscript()).isEqualTo("Patient mentioned sore back.");
    }

    @Test
    @DisplayName("Should order sessions by createdAt newest first")
    void newestFirstOrdering() {
        Instant earlier = Instant.parse("2026-11-11T10:00:00Z");
        Instant later = Instant.parse("2026-11-11T11:00:00Z");

        ScribeSession s1 = ScribeSession.createTranscribed(UUID.randomUUID(), "en", "First session", true, earlier);
        ScribeSession s2 = ScribeSession.createTranscribed(UUID.randomUUID(), "en", "Second session", true, later);

        sessionRepository.save(s1);
        sessionRepository.save(s2);
        sessionRepository.flush();

        List<ScribeSession> list = sessionRepository.findAllByOrderByCreatedAtDesc();
        assertThat(list).hasSizeGreaterThanOrEqualTo(2);
        assertThat(list.get(0).getCreatedAt()).isAfterOrEqualTo(list.get(1).getCreatedAt());
    }
}

