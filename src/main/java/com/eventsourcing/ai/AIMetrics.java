package com.eventsourcing.ai;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics tracking for AI service usage.
 */
public class AIMetrics {
    
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong rateLimits = new AtomicLong(0);
    private final AtomicLong totalTokens = new AtomicLong(0);
    private final Instant startTime = Instant.now();
    
    public void recordRequest(boolean success) {
        totalRequests.incrementAndGet();
        if (success) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
    }
    
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }
    
    public void recordRateLimit() {
        rateLimits.incrementAndGet();
    }
    
    public void recordTokenUsage(int tokens) {
        totalTokens.addAndGet(tokens);
    }
    
    public long getTotalRequests() { return totalRequests.get(); }
    public long getSuccessfulRequests() { return successfulRequests.get(); }
    public long getFailedRequests() { return failedRequests.get(); }
    public long getCacheHits() { return cacheHits.get(); }
    public long getRateLimits() { return rateLimits.get(); }
    public long getTotalTokens() { return totalTokens.get(); }
    
    public double getSuccessRate() {
        long total = getTotalRequests();
        return total > 0 ? (double) getSuccessfulRequests() / total : 0.0;
    }
    
    public double getCacheHitRate() {
        long totalWithCache = getTotalRequests() + getCacheHits();
        return totalWithCache > 0 ? (double) getCacheHits() / totalWithCache : 0.0;
    }
    
    public String getUptime() {
        var duration = java.time.Duration.between(startTime, Instant.now());
        var hours = duration.toHours();
        var minutes = duration.toMinutesPart();
        return String.format("%dh %dm", hours, minutes);
    }
}
