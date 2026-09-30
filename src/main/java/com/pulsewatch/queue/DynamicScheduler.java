package com.pulsewatch.queue;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pulsewatch.entity.Monitor;
import com.pulsewatch.repository.MonitorRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicScheduler {

    private final MonitorRepository monitorRepository;
    private final RedisQueueManager queueManager;

    @Scheduled(fixedDelay = 5000)
    @SchedulerLock(name = "dynamicScheduler_schedulePendingMonitors", lockAtMostFor = "PT1M", lockAtLeastFor = "PT4S")
    @Transactional
    public void schedulePendingMonitors() {
        List<Monitor> dueMonitors = monitorRepository.findDueForCheck();
        if (dueMonitors.isEmpty()) {
            return;
        }
        Instant enqueuedAt = Instant.now();
        for (Monitor m : dueMonitors) {
            queueManager.enqueue(new TaskPayload(
                    m.getId(),
                    m.getUrl(),
                    m.getTimeoutSeconds(),
                    m.getExpectedStatusCode()
            ));
            // Mark as enqueued immediately so the next tick never re-dispatches
            // the same monitor before its interval elapses (prevents task stampede).
            m.setLastCheckedAt(enqueuedAt);
        }
        monitorRepository.saveAll(dueMonitors);
        log.info("Dispatched {} monitors to Redis queue", dueMonitors.size());
    }
}