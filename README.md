# ⚡ PulseWatch

> Distributed, high-throughput website uptime and SSL certificate monitoring engine built with **Java 21 Virtual Threads**, **Spring Boot 3**, **Redis**, and **PostgreSQL**.

[![Live Demo](https://img.shields.io/badge/Live%20Demo-Render-46E3B7?style=for-the-badge&logo=render&logoColor=white)](https://pulsewatch-api-w2qi.onrender.com/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)

---

## 🎯 Overview

PulseWatch is an automated API telemetry and uptime monitoring system engineered for high concurrency and sub-second dispatching. Designed to eliminate thread-pool starvation typical of I/O-heavy polling architectures, PulseWatch pairs Java 21 Virtual Threads with an asynchronous Redis task queue to handle continuous non-blocking health checks, leaf SSL certificate expiration analysis, and automated incident alerting.

🔗 **Live Deployment:** [pulsewatch-api-w2qi.onrender.com](https://pulsewatch-api-w2qi.onrender.com/)

---

## 🏗️ System Architecture

```mermaid
graph TD
    UI[Frontend Dashboard] -->|REST API| API[Spring Boot Application Layer]
    
    subgraph Scheduling & Coordination
        SCHED[Dynamic Scheduler] -->|Distributed Lock| SL[(ShedLock via Redis)]
        SCHED -->|Enqueue Due Monitors| RQ[(Redis Task Queue: BRPOP)]
    end

    subgraph Probing Engine
        RQ -->|Fetch Task| WORKER[Daemon Worker Pool]
        WORKER -->|Virtual Thread per Probe| PROBE[Network Probe Engine]
        PROBE -->|DNS & IP Validation| SSRF[SSRF Guard]
        SSRF -->|HTTP / TLS Handshake| TARGET[Target Endpoint]
        PROBE -->|Capture Leaf Cert| X509[Custom X.509 TrustManager]
    end

    subgraph State & Incident Management
        PROBE -->|Record Latency & Status| EVAL[Incident Evaluator]
        EVAL -->|Pessimistic Lock SELECT FOR UPDATE| DB[(PostgreSQL)]
        EVAL -->|Trigger on 3 Consecutive Outages| NOTIF[Webhook Dispatcher]
        NOTIF -->|Linear Backoff Retry| DISCORD[Discord / Slack Alerts]
    end