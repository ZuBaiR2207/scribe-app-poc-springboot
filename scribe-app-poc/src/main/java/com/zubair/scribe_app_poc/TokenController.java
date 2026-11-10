package com.zubair.scribe_app_poc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RestController
public class TokenController {

    private final RestClient client = RestClient.create();

    @Value("${speechmatics.api-key}")
    private String apiKey;

    // Browser calls this to get a short-lived key (valid 60 seconds)
    @PostMapping("/api/token")
    public Map<String, String> createToken() {
        Map<?, ?> response = client.post()
                .uri("https://mp.speechmatics.com/v1/jwts?type=rt")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("ttl", 60))
                .retrieve()
                .body(Map.class);
        return Map.of("token", (String) response.get("key_value"));
    }
}