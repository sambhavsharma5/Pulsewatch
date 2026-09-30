package com.pulsewatch.dto;

import jakarta.validation.constraints.NotBlank;

public class DTOs {
    public record CreateMonitorRequest(
            @NotBlank String name,
            @NotBlank String url,
            Integer intervalSeconds,
            Integer timeoutSeconds,
            Integer expectedStatusCode,
            String webhookUrl
    ) {}

    public record MetricsResponse(
            double uptimePercentage,
            double averageLatencyMs,
            long totalChecks,
            java.util.List<com.pulsewatch.entity.Heartbeat> recentHeartbeats
    ) {}
}