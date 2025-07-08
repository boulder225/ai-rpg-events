package com.eventsourcing.gameSystem.core;

import java.util.Map;

/**
 * Response from processing a game action.
 */
public record GameResponse(
    boolean success,
    String message,
    GameContext updatedContext,
    Map<String, Object> additionalData,
    String error
) {
    
    /**
     * Create a successful response.
     */
    public static GameResponse success(String message, GameContext context) {
        return new GameResponse(true, message, context, Map.of(), null);
    }
    
    /**
     * Create a successful response with additional data.
     */
    public static GameResponse success(String message, GameContext context, Map<String, Object> data) {
        return new GameResponse(true, message, context, data, null);
    }
    
    /**
     * Create an error response.
     */
    public static GameResponse error(String error) {
        return new GameResponse(false, null, null, Map.of(), error);
    }
}