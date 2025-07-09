package com.eventsourcing.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data transfer objects for the AI-RPG API.
 * Using Records for immutability and type safety.
 */
public class ApiModels {
    
    /**
     * Request to create a new game session.
     */
    public record SessionCreateRequest(
        @JsonProperty("player_id") String playerId,
        @JsonProperty("player_name") String playerName
    ) {
        // Compact constructor for validation
        public SessionCreateRequest {
            if (playerId == null || playerId.trim().isEmpty()) {
                throw new IllegalArgumentException("Player ID cannot be null or empty");
            }
            if (playerName == null || playerName.trim().isEmpty()) {
                throw new IllegalArgumentException("Player name cannot be null or empty");
            }
        }
    }
    
    /**
     * Request to perform a game action.
     */
    public record GameActionRequest(
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("command") String command
    ) {
        // Compact constructor for validation
        public GameActionRequest {
            if (sessionId == null || sessionId.trim().isEmpty()) {
                throw new IllegalArgumentException("Session ID cannot be null or empty");
            }
            if (command == null || command.trim().isEmpty()) {
                throw new IllegalArgumentException("Command cannot be null or empty");
            }
        }
    }
    
    /**
     * Response from game action processing.
     */
    public record GameResponse(
        @JsonProperty("success") boolean success,
        @JsonProperty("message") String message,
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("context") Object context,
        @JsonProperty("error") String error
    ) {
        /**
         * Create a successful response.
         */
        public static GameResponse success(String message, String sessionId, Object context) {
            return new GameResponse(true, message, sessionId, context, null);
        }
        
        /**
         * Create a successful response without context.
         */
        public static GameResponse success(String message, String sessionId) {
            return new GameResponse(true, message, sessionId, null, null);
        }
        
        /**
         * Create an error response.
         */
        public static GameResponse error(String error, String sessionId) {
            return new GameResponse(false, null, sessionId, null, error);
        }
        
        /**
         * Create an error response without session ID.
         */
        public static GameResponse error(String error) {
            return new GameResponse(false, null, null, null, error);
        }
    }
}
