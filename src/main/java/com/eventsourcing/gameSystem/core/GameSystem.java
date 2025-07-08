package com.eventsourcing.gameSystem.core;

import java.util.List;
import java.util.Map;

/**
 * Core interface for game system plugins.
 * Enables the platform to support multiple RPG systems (D&D, Cthulhu, Cyberpunk, etc.)
 * while maintaining the same event sourcing and AI integration capabilities.
 */
public interface GameSystem {
    
    /**
     * Get the display name of this game system.
     * @return Human-readable name (e.g., "D&D Basic", "Call of Cthulhu")
     */
    String getSystemName();
    
    /**
     * Get a brief description of this game system.
     * @return Description for UI display
     */
    String getSystemDescription();
    
    /**
     * Load adventure data for the specified adventure.
     * @param adventureId The adventure identifier
     * @return Complete adventure data including locations, NPCs, etc.
     */
    AdventureData loadAdventure(String adventureId);
    
    /**
     * Create initial game context for a new player.
     * @param playerName The player's character name
     * @return Initial game context with starting location and stats
     */
    GameContext createInitialContext(String playerName);
    
    /**
     * Process a game action command within the given context.
     * @param context Current game state
     * @param command Player command (e.g., "/look around", "/attack goblin")
     * @return Game response with results and updated context
     */
    GameResponse processAction(GameContext context, String command);
    
    /**
     * Get location data for the frontend map display.
     * @return Map of location ID to location display data
     */
    Map<String, LocationData> getLocationData();
    
    /**
     * Get quick action commands for the frontend UI.
     * @return List of commonly used commands with display labels
     */
    List<QuickCommand> getQuickCommands();
    
    /**
     * Get the default starting location for new characters.
     * @return Location ID where players begin
     */
    String getStartingLocation();
    
    /**
     * Get a human-readable context summary for the current adventure/location.
     * @param adventure The adventure data
     * @param locationId The current location ID
     * @return Context string for AI or frontend
     */
    String getAdventureContext(AdventureData adventure, String locationId);

    /**
     * Get a human-readable summary of the game system rules.
     * @return Rules context string for AI or frontend
     */
    String getRulesContext();

    /**
     * Get metadata about the game system for the frontend (name, description, commands, etc).
     * @return Map of metadata fields
     */
    Map<String, Object> getMetadata();
}