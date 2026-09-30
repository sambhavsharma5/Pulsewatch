package com.pulsewatch.queue;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisQueueManager {

    private static final String QUEUE_KEY = "pulsewatch:tasks:probe";
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void enqueue(TaskPayload task) {
        try {
            String json = objectMapper.writeValueAsString(task);
            redisTemplate.opsForList().leftPush(QUEUE_KEY, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize task: {}", e.getMessage());
        }
    }

    public TaskPayload blockingPop(Duration timeout) {
        try {
            String json = redisTemplate.opsForList().rightPop(QUEUE_KEY, timeout);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, TaskPayload.class);
        } catch (JsonProcessingException e) {
            log.error("Corrupted task payload skipped: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error reading from Redis queue: {}", e.getMessage());
            return null;
        }
    }
}