package com.focusflow.service;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance, concurrent service for tracking application traffic,
 * API endpoint utilization, response status metrics, JVM runtime diagnostics,
 * HikariCP connection pool health, and Micrometer dimensional telemetry.
 */
@Service
public class TrafficMetricsService {

    private final long appStartTime = System.currentTimeMillis();
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalLatencyMs = new AtomicLong(0);

    private final ConcurrentHashMap<String, AtomicLong> endpointHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, AtomicLong> statusCodes = new ConcurrentHashMap<>();

    private final MeterRegistry meterRegistry;
    private final DataSource dataSource;

    private Counter requestCounter;
    private Timer requestTimer;

    @Autowired
    public TrafficMetricsService(Optional<MeterRegistry> meterRegistryOpt,
                                 Optional<DataSource> dataSourceOpt) {
        this.meterRegistry = meterRegistryOpt.orElse(null);
        this.dataSource = dataSourceOpt.orElse(null);

        if (this.meterRegistry != null) {
            this.requestCounter = Counter.builder("focusflow.http.requests.total")
                    .description("Total inbound HTTP requests processed by FocusFlow")
                    .register(this.meterRegistry);

            this.requestTimer = Timer.builder("focusflow.http.requests.duration")
                    .description("Execution latency for inbound HTTP requests")
                    .publishPercentiles(0.5, 0.9, 0.99)
                    .register(this.meterRegistry);
        }
    }

    public void recordRequest(String uri, int status, long durationMs) {
        totalRequests.incrementAndGet();
        totalLatencyMs.addAndGet(durationMs);

        // Record in Micrometer if active
        if (requestCounter != null) {
            requestCounter.increment();
        }
        if (requestTimer != null) {
            requestTimer.record(durationMs, TimeUnit.MILLISECONDS);
        }

        // Normalize URI to prevent cardinality explosion (strip specific IDs)
        String normalizedUri = normalizeUri(uri);
        endpointHits.computeIfAbsent(normalizedUri, k -> new AtomicLong(0)).incrementAndGet();

        int statusGroup = (status / 100) * 100;
        statusCodes.computeIfAbsent(statusGroup, k -> new AtomicLong(0)).incrementAndGet();
    }

    private String normalizeUri(String uri) {
        if (uri == null) return "/";
        return uri.replaceAll("/\\d+", "/{id}");
    }

    public Map<String, Object> getTrafficAnalytics() {
        Map<String, Object> traffic = new LinkedHashMap<>();
        long total = totalRequests.get();
        traffic.put("totalRequests", total);

        long uptimeSeconds = (System.currentTimeMillis() - appStartTime) / 1000;
        double reqPerSec = uptimeSeconds > 0 ? (double) total / uptimeSeconds : total;
        traffic.put("requestsPerSecond", Math.round(reqPerSec * 100.0) / 100.0);
        traffic.put("requestsPerMinute", Math.round(reqPerSec * 60.0 * 10.0) / 10.0);

        long avgLatency = total > 0 ? totalLatencyMs.get() / total : 0;
        traffic.put("avgLatencyMs", avgLatency);

        // Top 8 endpoints
        List<Map<String, Object>> topEndpoints = new ArrayList<>();
        endpointHits.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get()))
                .limit(8)
                .forEach(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("endpoint", e.getKey());
                    item.put("count", e.getValue().get());
                    topEndpoints.add(item);
                });
        traffic.put("topEndpoints", topEndpoints);

        // Status code distribution
        Map<String, Long> statusDist = new HashMap<>();
        statusDist.put("2xx", statusCodes.getOrDefault(200, new AtomicLong(0)).get());
        statusDist.put("4xx", statusCodes.getOrDefault(400, new AtomicLong(0)).get());
        statusDist.put("5xx", statusCodes.getOrDefault(500, new AtomicLong(0)).get());
        traffic.put("statusDistribution", statusDist);

        return traffic;
    }

    public Map<String, Object> getServiceHealthMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        long uptimeSeconds = (System.currentTimeMillis() - appStartTime) / 1000;
        metrics.put("uptimeSeconds", uptimeSeconds);
        metrics.put("status", "UP");

        // Database connection pool statistics (HikariCP)
        Map<String, Object> dbPool = new LinkedHashMap<>();
        if (dataSource instanceof HikariDataSource hikariDs) {
            HikariPoolMXBean poolMx = hikariDs.getHikariPoolMXBean();
            if (poolMx != null) {
                dbPool.put("activeConnections", poolMx.getActiveConnections());
                dbPool.put("idleConnections", poolMx.getIdleConnections());
                dbPool.put("totalConnections", poolMx.getTotalConnections());
                dbPool.put("threadsAwaitingConnection", poolMx.getThreadsAwaitingConnection());
            }
            dbPool.put("maxPoolSize", hikariDs.getMaximumPoolSize());
            dbPool.put("poolName", hikariDs.getPoolName());
            metrics.put("databaseStatus", "CONNECTED (HikariCP)");
        } else {
            metrics.put("databaseStatus", "CONNECTED");
            dbPool.put("status", "ACTIVE");
        }
        metrics.put("connectionPool", dbPool);

        // JVM Memory Diagnostics
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long heapMax = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
        long heapCommitted = memoryBean.getHeapMemoryUsage().getCommitted() / (1024 * 1024);

        metrics.put("heapUsedMb", heapUsed);
        metrics.put("heapMaxMb", heapMax);
        metrics.put("heapCommittedMb", heapCommitted);

        // Concurrency & Compute
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        metrics.put("threadCount", threadBean.getThreadCount());
        metrics.put("peakThreadCount", threadBean.getPeakThreadCount());
        metrics.put("availableProcessors", Runtime.getRuntime().availableProcessors());

        // Storage / Disk space
        File rootDisk = new File(".");
        long freeDiskGb = rootDisk.getFreeSpace() / (1024 * 1024 * 1024);
        long totalDiskGb = rootDisk.getTotalSpace() / (1024 * 1024 * 1024);
        metrics.put("diskFreeGb", freeDiskGb);
        metrics.put("diskTotalGb", totalDiskGb);

        // Micrometer & Prometheus indicator
        metrics.put("micrometerEnabled", meterRegistry != null);
        metrics.put("prometheusEndpoint", "/actuator/prometheus");

        return metrics;
    }
}
