package com.pulsewatch.queue;

import java.util.List;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pulsewatch.entity.Monitor;
import com.pulsewatch.repository.MonitorRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class DynamicScheduler {

    private final MonitorRepository monitorRepository;
    private final RedisQueueManager queueManager;

    @Scheduled(fixedDelay = 5000)
    public void schedulePendingMonitors() {
        List<Monitor> dueMonitors = monitorRepository.findDueForCheck();
        for (Monitor m : dueMonitors) {
            queueManager.enqueue(new TaskPayload(
                    m.getId(),
                    m.getUrl(),
                    m.getTimeoutSeconds(),
                    m.getExpectedStatusCode()
            ));
        }
        if (!dueMonitors.isEmpty()) {
            log.info("Dispatched {} monitors to Redis queue", dueMonitors.size());
        }
    }
}