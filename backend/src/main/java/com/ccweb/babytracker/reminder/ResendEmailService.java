package com.ccweb.babytracker.reminder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ResendEmailService {

    private final String apiKey;
    private final String from;
    private final RestTemplate restTemplate;

    public ResendEmailService(@Value("${app.resend.api-key}") String apiKey,
                              @Value("${app.resend.from}") String from) {
        this.apiKey = apiKey;
        this.from = from;
        this.restTemplate = new RestTemplate();
    }

    public void send(String to, String subject, String html) {
        if (apiKey == null || apiKey.isBlank()) return; // no-op in dev/test

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "from", from,
                "to", new String[]{to},
                "subject", subject,
                "html", html
        );

        restTemplate.postForObject("https://api.resend.com/emails",
                new HttpEntity<>(body, headers), Map.class);
    }
}
