package com.eventsourcing.gameSystem.core;

import java.util.Map;

/**
 * Generic game context that holds current game state information.
 * Used by game systems to track player state and game progress.
 */
public record GameContext(
    String playerId,
    String playerName,
    String currentLocation,
    int health,
    int maxHealth,
    Map<String, Integer> stats,
    Map<String, String> characterData,
    Map<String, Object> gameSpecificData
) {
    
    /**
     * Create a new context with updated location.
     * @param newLocation The new location ID
     * @return New GameContext with updated location
     */
    public GameContext withLocation(String newLocation) {
        return new GameContext(
            playerId, playerName, newLocation, health, maxHealth,
            stats, characterData, gameSpecificData
        );
    }
    
    /**
     * Create a new context with updated health.
     * @param newHealth The new health value
     * @return New GameContext with updated health
     */
    public GameContext withHealth(int newHealth) {
        return new GameContext(
            playerId, playerName, currentLocation, newHealth, maxHealth,
            stats, characterData, gameSpecificData
        );
    }
    
    /**
     * Get a character data value.
     * @param key The data key
     * @return The value or null if not found
     */
    public String getCharacterData(String key) {
        return characterData.get(key);
    }
    
    /**
     * Get a stat value.
     * @param statName The stat name
     * @return The stat value or 0 if not found
     */
    public int getStat(String statName) {
        return stats.getOrDefault(statName, 0);
    }
    
    /**
     * Check if the character is alive.
     * @return true if health > 0
     */
    public boolean isAlive() {
        return health > 0;
    }
}