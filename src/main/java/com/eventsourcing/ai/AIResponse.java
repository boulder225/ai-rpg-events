package com.eventsourcing.ai;

import java.time.Instant;

/**
 * Response from AI service with type-safe variants.
 * Uses sealed interface pattern for exhaustive handling.
 */
public sealed interface AIResponse permits AIResponse.Success, AIResponse.Fallback, AIResponse.Error {
    
    /**
     * Timestamp when the response was generated.
     */
    Instant timestamp();
    
    /**
     * Content of the response (may be empty for errors).
     */
    String content();
    
    /**
     * Check if this response represents a successful AI generation.
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }
    
    /**
     * Successful AI response with token usage information.
     */
    record Success(
        String content,
        int inputTokens,
        int outputTokens,
        Instant timestamp
    ) implements AIResponse {
        
        /**
         * Total tokens consumed by this request.
         */
        public int getTotalTokens() {
            return inputTokens + outputTokens;
        }
    }
    
    /**
     * Fallback response when AI service is unavailable or rate limited.
     */
    record Fallback(
        String content,
        String reason,
        Instant timestamp
    ) implements AIResponse {}
    
    /**
     * Error response when AI service encounters an error.
     */
    record Error(
        String errorMessage,
        Instant timestamp
    ) implements AIResponse {
        
        /**
         * Error responses have no content.
         */
        @Override
        public String content() {
            return "";
        }
    }
    
    // Factory methods for easy creation
    
    /**
     * Create a successful AI response.
     */
    static AIResponse success(String content, int inputTokens, int outputTokens) {
        return new Success(content, inputTokens, outputTokens, Instant.now());
    }
    
    /**
     * Create a fallback response with reason.
     */
    static AIResponse fallback(String content, String reason) {
        return new Fallback(content, reason, Instant.now());
    }
    
    /**
     * Create an error response.
     */
    static AIResponse error(String errorMessage) {
        return new Error(errorMessage, Instant.now());
    }
}
