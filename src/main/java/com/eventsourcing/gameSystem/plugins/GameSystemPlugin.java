package com.eventsourcing.gameSystem.plugins;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface for game system plugins that extend the generic context manager
 * with game-specific rules and behaviors.
 */
public interface GameSystemPlugin {
    
    /**
     * @return The name of this plugin
     */
    String getName();
    
    /**
     * @return The version of this plugin
     */
    String getVersion();
    
    /**
     * Enhance the base context with game-specific information
     * 
     * @param baseContext The basic context from the generic manager
     * @return Enhanced context with game-specific additions
     */
    JsonNode enhanceContext(JsonNode baseContext);
    
    /**
     * Validate an action against game-specific rules
     * 
     * @param action The action to validate
     * @param currentContext The current game context
     * @return Validation result with any violations or suggestions
     */
    ValidationResult validateAction(JsonNode action, JsonNode currentContext);
    
    /**
     * Called before state updates are applied, allows plugin to modify or prepare
     * 
     * @param changes The proposed state changes
     * @param currentContext The current game context
     */
    void beforeStateUpdate(JsonNode changes, JsonNode currentContext);
    
    /**
     * Called after state updates are applied, allows plugin to handle consequences
     * 
     * @param changes The state changes that were applied
     * @param newContext The updated game context
     */
    void afterStateUpdate(JsonNode changes, JsonNode newContext);
    
    /**
     * Enhance the AI prompt with game-specific rules and context
     * 
     * @param basePrompt The basic prompt template
     * @param context The current game context
     * @return Enhanced prompt with game-specific additions
     */
    String enhancePrompt(String basePrompt, JsonNode context);
    
    /**
     * Check if this plugin can handle the given game system
     * 
     * @param gameSystemType The type of game system
     * @return true if this plugin supports the game system
     */
    boolean supportsGameSystem(String gameSystemType);
}
