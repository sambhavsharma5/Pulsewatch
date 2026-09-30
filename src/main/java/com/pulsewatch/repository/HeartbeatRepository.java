package com.pulsewatch.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pulsewatch.entity.Heartbeat;

@Repository
public interface HeartbeatRepository extends JpaRepository<Heartbeat, UUID> {
    List<Heartbeat> findTop50ByMonitorIdOrderByCreatedAtDesc(UUID monitorId);

    @Query("SELECT COUNT(h) FROM Heartbeat h WHERE h.monitorId = :monitorId AND h.createdAt >= :since")
    long countChecksSince(@Param("monitorId") UUID monitorId, @Param("since") Instant since);

    @Query("SELECT COUNT(h) FROM Heartbeat h WHERE h.monitorId = :monitorId AND h.isSuccessful = true AND h.createdAt >= :since")
    long countSuccessfulChecksSince(@Param("monitorId") UUID monitorId, @Param("since") Instant since);

    @Query("SELECT AVG(h.latencyMs) FROM Heartbeat h WHERE h.monitorId = :monitorId AND h.createdAt >= :since")
    Double getAverageLatencySince(@Param("monitorId") UUID monitorId, @Param("since") Instant since);
}