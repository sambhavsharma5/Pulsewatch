package com.pulsewatch.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pulsewatch.entity.Monitor;

import jakarta.persistence.LockModeType;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, UUID> {
    List<Monitor> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Monitor m WHERE m.id = :id")
    Optional<Monitor> findByIdForUpdate(@Param("id") UUID id);

    @Query(value = """
        SELECT * FROM monitors
        WHERE last_checked_at IS NULL
           OR (last_checked_at + (interval_seconds * INTERVAL '1 second')) <= NOW()
    """, nativeQuery = true)
    List<Monitor> findDueForCheck();
}