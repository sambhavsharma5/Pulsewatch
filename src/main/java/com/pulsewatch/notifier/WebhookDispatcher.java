package com.pulsewatch.notifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDispatcher {

    private final ObjectMapper objectMapper;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public void dispatch(String webhookUrl, String event, String monitorName, String targetUrl, String reason) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "content", String.format("🚨 **[%s]** `%s` (%s)\n> **Status:** %s", event, monitorName, targetUrl, reason),
                    "event", event,
                    "monitor_name", monitorName,
                    "target_url", targetUrl,
                    "reason", reason,
                    "timestamp", java.time.Instant.now().toString()
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            // Run delivery with linear backoff (3 attempts)
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    HttpResponse<Void> resp = client.send(request, HttpResponse.BodyHandlers.discarding());
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        return;
                    }
                } catch (Exception e) {
                    if (attempt == 3) throw e;
                    Thread.sleep(attempt * 500L);
                }
            }
        } catch (Exception e) {
            log.error("Failed delivering alert payload to endpoint {}: {}", webhookUrl, e.getMessage());
        }
    }
}