package com.eventsourcing.ai;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple cache for AI responses to reduce API calls and improve performance.
 */
public class AICache {
    
    private final ConcurrentHashMap<String, CacheEntry> cache;
    private final int ttlMinutes;
    
    public AICache(int ttlMinutes) {
        this.cache = new ConcurrentHashMap<>();
        this.ttlMinutes = ttlMinutes;
    }
    
    public AIResponse get(String key) {
        var entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        
        if (Instant.now().isAfter(entry.expiryTime)) {
            cache.remove(key);
            return null;
        }
        
        return entry.response;
    }
    
    public void put(String key, AIResponse response) {
        var expiryTime = Instant.now().plusSeconds(ttlMinutes * 60L);
        cache.put(key, new CacheEntry(response, expiryTime));
    }
    
    public void clear() {
        cache.clear();
    }
    
    public int size() {
        return cache.size();
    }
    
    private static class CacheEntry {
        final AIResponse response;
        final Instant expiryTime;
        
        CacheEntry(AIResponse response, Instant expiryTime) {
            this.response = response;
            this.expiryTime = expiryTime;
        }
    }
} 