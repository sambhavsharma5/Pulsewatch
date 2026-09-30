package com.pulsewatch.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pulsewatch.entity.Incident;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    Optional<Incident> findFirstByMonitorIdAndResolvedAtIsNullOrderByStartedAtDesc(UUID monitorId);
}