package com.pulsewatch.queue;

import java.util.UUID;

public record TaskPayload(
        UUID monitorId,
        String targetUrl,
        int timeoutSeconds,
        int expectedStatusCode
) {}