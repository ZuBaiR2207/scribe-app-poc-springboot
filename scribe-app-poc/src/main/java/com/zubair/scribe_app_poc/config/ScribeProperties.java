package com.zubair.scribe_app_poc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record ScribeProperties(
        Limits limits,
        List<String> allowedLanguages
) {
    public ScribeProperties {
        if (limits == null) {
            limits = new Limits(50000);
        }
        if (allowedLanguages == null || allowedLanguages.isEmpty()) {
            allowedLanguages = List.of("cmn_en_ms_ta", "en_ms", "cmn_en", "en_ta", "en", "ms");
        }
    }

    public record Limits(int maxTranscriptLength) {
        public Limits {
            if (maxTranscriptLength <= 0) {
                maxTranscriptLength = 50000;
            }
        }
    }
}

