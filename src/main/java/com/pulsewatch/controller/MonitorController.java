package com.pulsewatch.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewatch.dto.DTOs.CreateMonitorRequest;
import com.pulsewatch.dto.DTOs.MetricsResponse;
import com.pulsewatch.dto.DTOs.UpdateMonitorRequest;
import com.pulsewatch.entity.Monitor;
import com.pulsewatch.probe.SsrfGuard;
import com.pulsewatch.repository.HeartbeatRepository;
import com.pulsewatch.repository.MonitorRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/monitors")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorRepository monitorRepository;
    private final HeartbeatRepository heartbeatRepository;
    private final SsrfGuard ssrfGuard;

    @PostMapping
    public ResponseEntity<?> createMonitor(@RequestBody @Valid CreateMonitorRequest req) {
        try {
            ssrfGuard.assertSafeUrl(req.url());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        Monitor monitor = Monitor.builder()
                .name(req.name())
                .url(req.url())
                .intervalSeconds(req.intervalSeconds() != null ? Math.max(5, req.intervalSeconds()) : 60)
                .timeoutSeconds(req.timeoutSeconds() != null ? req.timeoutSeconds() : 5)
                .expectedStatusCode(req.expectedStatusCode() != null ? req.expectedStatusCode() : 200)
                .webhookUrl(req.webhookUrl())
                .status("PENDING")
                .consecutiveFailures(0)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(monitorRepository.save(monitor));
    }

    @GetMapping
    public ResponseEntity<List<Monitor>> listMonitors() {
        return ResponseEntity.ok(monitorRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<MetricsResponse> getMetrics(@PathVariable UUID id) {
        Monitor monitor = monitorRepository.findById(id).orElse(null);
        if (monitor == null) {
            return ResponseEntity.notFound().build();
        }

        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        long totalChecks = heartbeatRepository.countChecksSince(id, since);
        long successChecks = heartbeatRepository.countSuccessfulChecksSince(id, since);
        Double avgLatency = heartbeatRepository.getAverageLatencySince(id, since);

        double uptime = (totalChecks == 0) ? 100.0 : ((double) successChecks / totalChecks) * 100.0;

        return ResponseEntity.ok(new MetricsResponse(
                uptime,
                avgLatency != null ? avgLatency : 0.0,
                totalChecks,
                heartbeatRepository.findTop50ByMonitorIdOrderByCreatedAtDesc(id)
        ));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateMonitor(@PathVariable UUID id, @RequestBody @Valid UpdateMonitorRequest req) {
        Monitor monitor = monitorRepository.findById(id).orElse(null);
        if (monitor == null) {
            return ResponseEntity.notFound().build();
        }

        if (req.name() != null && !req.name().isBlank()) {
            monitor.setName(req.name());
        }
        if (req.url() != null && !req.url().isBlank()) {
            try {
                ssrfGuard.assertSafeUrl(req.url());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
            monitor.setUrl(req.url());
        }
        if (req.intervalSeconds() != null) {
            monitor.setIntervalSeconds(Math.max(5, req.intervalSeconds()));
        }
        if (req.timeoutSeconds() != null) {
            monitor.setTimeoutSeconds(req.timeoutSeconds());
        }
        if (req.expectedStatusCode() != null) {
            monitor.setExpectedStatusCode(req.expectedStatusCode());
        }
        if (req.webhookUrl() != null) {
            monitor.setWebhookUrl(req.webhookUrl());
        }
        if (req.enabled() != null) {
            monitor.setEnabled(req.enabled());
        }

        return ResponseEntity.ok(monitorRepository.save(monitor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(@PathVariable UUID id) {
        if (!monitorRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        monitorRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}