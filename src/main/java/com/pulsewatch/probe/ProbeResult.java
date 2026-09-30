package com.pulsewatch.probe;

public record ProbeResult(
        Integer statusCode,
        int latencyMs,
        boolean isSuccessful,
        Integer sslDaysRemaining,
        String errorMessage
) {}