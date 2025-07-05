package com.eventsourcing.api;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics tracking for the AI-RPG platform.
 * Monitors system performance and usage statistics.
 */
public class RPGMetrics {
    
    private final AtomicLong totalSessions = new AtomicLong(0);
    private final AtomicLong totalActions = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicLong responseCount = new AtomicLong(0);
    private final Instant startTime = Instant.now();
    
    public void incrementSessions() {
        totalSessions.incrementAndGet();
    }
    
    public void incrementActions() {
        totalActions.incrementAndGet();
    }
    
    public void recordResponseTime(long milliseconds) {
        totalResponseTime.addAndGet(milliseconds);
        responseCount.incrementAndGet();
    }
    
    public long getTotalSessions() {
        return totalSessions.get();
    }
    
    public long getTotalActions() {
        return totalActions.get();
    }
    
    public double getAverageResponseTime() {
        long count = responseCount.get();
        return count > 0 ? (double) totalResponseTime.get() / count : 0.0;
    }
    
    public String getUptime() {
        var duration = java.time.Duration.between(startTime, Instant.now());
        var hours = duration.toHours();
        var minutes = duration.toMinutesPart();
        var seconds = duration.toSecondsPart();
        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }
}
