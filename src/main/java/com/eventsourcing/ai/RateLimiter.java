package com.eventsourcing.ai;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple rate limiter for AI API calls.
 * Tracks requests per minute and blocks when limit is exceeded.
 */
public class RateLimiter {
    
    private final int requestsPerMinute;
    private final AtomicInteger requestCount;
    private volatile long windowStart;
    
    public RateLimiter(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
        this.requestCount = new AtomicInteger(0);
        this.windowStart = Instant.now().getEpochSecond();
    }
    
    public boolean tryAcquire() {
        long currentTime = Instant.now().getEpochSecond();
        long window = currentTime / 60; // 1-minute windows
        
        if (window != windowStart) {
            // New window, reset counter
            windowStart = window;
            requestCount.set(0);
        }
        
        return requestCount.incrementAndGet() <= requestsPerMinute;
    }
    
    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }
    
    public int getCurrentCount() {
        return requestCount.get();
    }
} 