package com.eventsourcing.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Logging utility for the AI-RPG platform.
 * Provides contextual logging with MDC support for tracking player sessions and AI requests.
 */
public class RPGLogger {
    
    /**
     * Get a logger for the specified class.
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
    
    /**
     * Set player context for logging correlation.
     * This will appear in all subsequent log messages until cleared.
     */
    public static void setPlayerContext(String playerId, String sessionId) {
        MDC.put("playerId", playerId);
        if (sessionId != null) {
            MDC.put("sessionId", sessionId);
        }
    }
    
    /**
     * Set AI context for logging correlation.
     * Used to track AI requests and responses.
     */
    public static void setAIContext(String model, String requestId) {
        MDC.put("aiModel", model);
        MDC.put("requestId", requestId);
    }
    
    /**
     * Set game context for event logging.
     */
    public static void setGameContext(String location, String action) {
        if (location != null) {
            MDC.put("location", location);
        }
        if (action != null) {
            MDC.put("action", action);
        }
    }
    
    /**
     * Clear all MDC context.
     * Should be called when request processing is complete.
     */
    public static void clearContext() {
        MDC.clear();
    }
    
    /**
     * Clear specific context keys while keeping others.
     */
    public static void clearPlayerContext() {
        MDC.remove("playerId");
        MDC.remove("sessionId");
    }
    
    public static void clearAIContext() {
        MDC.remove("aiModel");
        MDC.remove("requestId");
    }
    
    public static void clearGameContext() {
        MDC.remove("location");
        MDC.remove("action");
    }
}
