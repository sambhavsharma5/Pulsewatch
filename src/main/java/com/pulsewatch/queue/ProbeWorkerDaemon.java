package com.pulsewatch.queue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;

import com.pulsewatch.incident.IncidentEvaluator;
import com.pulsewatch.probe.NetworkProbeEngine;
import com.pulsewatch.probe.ProbeResult;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProbeWorkerDaemon {

    private final RedisQueueManager queueManager;
    private final NetworkProbeEngine probeEngine;
    private final IncidentEvaluator incidentEvaluator;
    
    // Concurrency pool using Java 21 Virtual Threads
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @PostConstruct
    public void startWorkers() {
        // Launch 5 queue-consumer loopers powered by Virtual Threads
        for (int i = 0; i < 5; i++) {
            executor.submit(this::processQueueLoop);
        }
        log.info("Initialized 5 Virtual-Thread queue listeners.");
    }

    private void processQueueLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                TaskPayload task = queueManager.pop();
                if (task != null) {
                    executor.submit(() -> {
                        ProbeResult res = probeEngine.execute(
                                task.targetUrl(),
                                task.timeoutSeconds(),
                                task.expectedStatusCode()
                        );
                        incidentEvaluator.evaluate(task.monitorId(), res);
                    });
                } else {
                    // Queue is empty: sleep 1 second without consuming OS threads
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Worker processing loop error: {}", e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}