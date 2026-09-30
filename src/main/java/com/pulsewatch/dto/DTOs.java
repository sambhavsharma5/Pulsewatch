package com.pulsewatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DTOs {
    public record CreateMonitorRequest(
            @NotBlank @Size(max = 128) String name,
            @NotBlank String url,
            Integer intervalSeconds,
            Integer timeoutSeconds,
            Integer expectedStatusCode,
            String webhookUrl
    ) {}

    public record UpdateMonitorRequest(
            @Size(min = 1, max = 128) String name,
            String url,
            Integer intervalSeconds,
            Integer timeoutSeconds,
            Integer expectedStatusCode,
            String webhookUrl,
            Boolean enabled
    ) {}

    public record MetricsResponse(
            double uptimePercentage,
            double averageLatencyMs,
            long totalChecks,
            java.util.List<com.pulsewatch.entity.Heartbeat> recentHeartbeats
    ) {}
}