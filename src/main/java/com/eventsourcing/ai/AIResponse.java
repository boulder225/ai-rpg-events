package com.eventsourcing.ai;

import java.time.Instant;

/**
 * Response from AI service with metadata.
 */
public record AIResponse(
    String content,
    boolean fromAI,
    String errorMessage,
    int inputTokens,
    int outputTokens,
    Instant timestamp
) {
    
    public boolean isSuccess() {
        return fromAI && errorMessage == null;
    }
    
    public int getTotalTokens() {
        return inputTokens + outputTokens;
    }
    
    public static AIResponse success(String content, int inputTokens, int outputTokens) {
        return new AIResponse(content, true, null, inputTokens, outputTokens, Instant.now());
    }
    
    public static AIResponse fallback(String content, String reason) {
        return new AIResponse(content, false, reason, 0, 0, Instant.now());
    }
    
    public static AIResponse error(String errorMessage) {
        return new AIResponse("", false, errorMessage, 0, 0, Instant.now());
    }
}
