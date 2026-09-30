package com.pulsewatch.incident;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewatch.entity.Heartbeat;
import com.pulsewatch.entity.Incident;
import com.pulsewatch.entity.Monitor;
import com.pulsewatch.notifier.WebhookDispatcher;
import com.pulsewatch.probe.ProbeResult;
import com.pulsewatch.repository.HeartbeatRepository;
import com.pulsewatch.repository.IncidentRepository;
import com.pulsewatch.repository.MonitorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IncidentEvaluator {

    public static final int CONSECUTIVE_THRESHOLD = 3;

    private final MonitorRepository monitorRepository;
    private final HeartbeatRepository heartbeatRepository;
    private final IncidentRepository incidentRepository;
    private final WebhookDispatcher webhookDispatcher;

    @Transactional
    public void evaluate(UUID monitorId, ProbeResult res) {
        // 1. Write the Heartbeat row
        Heartbeat heartbeat = Heartbeat.builder()
                .monitorId(monitorId)
                .statusCode(res.statusCode())
                .latencyMs(res.latencyMs())
                .isSuccessful(res.isSuccessful())
                .sslDaysRemaining(res.sslDaysRemaining())
                .errorMessage(res.errorMessage())
                .build();
        heartbeatRepository.save(heartbeat);

        // 2. Fetch Monitor with row-level lock (FOR UPDATE)
        Monitor monitor = monitorRepository.findByIdForUpdate(monitorId)
                .orElseThrow(() -> new IllegalArgumentException("Monitor " + monitorId + " not found"));

        Instant now = Instant.now();
        monitor.setLastCheckedAt(now);

        if (res.isSuccessful()) {
            if ("DOWN".equals(monitor.getStatus())) {
                // Recovered from an outage
                incidentRepository.findFirstByMonitorIdAndResolvedAtIsNullOrderByStartedAtDesc(monitorId)
                        .ifPresent(inc -> inc.setResolvedAt(now));

                webhookDispatcher.dispatch(
                        monitor.getWebhookUrl(),
                        "INCIDENT_RESOLVED",
                        monitor.getName(),
                        monitor.getUrl(),
                        "Service recovered: Status " + res.statusCode() + " received."
                );
            }
            monitor.setStatus("UP");
            monitor.setConsecutiveFailures(0);
        } else {
            int failures = monitor.getConsecutiveFailures() + 1;
            monitor.setConsecutiveFailures(failures);

            if (failures >= CONSECUTIVE_THRESHOLD && !"DOWN".equals(monitor.getStatus())) {
                monitor.setStatus("DOWN");

                Incident incident = Incident.builder()
                        .monitorId(monitorId)
                        .startedAt(now)
                        .cause(res.errorMessage() != null ? res.errorMessage() : "Unknown failure")
                        .build();
                incidentRepository.save(incident);

                webhookDispatcher.dispatch(
                        monitor.getWebhookUrl(),
                        "INCIDENT_TRIGGERED",
                        monitor.getName(),
                        monitor.getUrl(),
                        res.errorMessage()
                );
            }
        }

        monitorRepository.save(monitor);
    }
}