package com.eventsourcing.gameSystem.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Simple factory for creating game system instances.
 * Supports configuration-driven game system loading.
 */
public class GameSystemFactory {
    
    private static final Map<String, Class<? extends GameSystem>> REGISTERED_SYSTEMS = new HashMap<>();
    
    // Static registration of available game systems
    static {
        // D&D system will be registered here when we create the plugin
        // registerSystem("dnd", DnDGameSystem.class);
        // registerSystem("cthulhu", CthulhuGameSystem.class);
    }
    
    /**
     * Register a game system implementation.
     * @param systemId The system identifier (e.g., "dnd", "cthulhu")
     * @param systemClass The GameSystem implementation class
     */
    public static void registerSystem(String systemId, Class<? extends GameSystem> systemClass) {
        REGISTERED_SYSTEMS.put(systemId, systemClass);
    }
    
    /**
     * Create a game system instance by ID.
     * @param systemId The system identifier
     * @return GameSystem instance
     * @throws IllegalArgumentException if system not found
     */
    public static GameSystem createSystem(String systemId) {
        Class<? extends GameSystem> systemClass = REGISTERED_SYSTEMS.get(systemId);
        if (systemClass == null) {
            throw new IllegalArgumentException("Unknown game system: " + systemId);
        }
        
        try {
            return systemClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create game system: " + systemId, e);
        }
    }
    
    /**
     * Create a game system from configuration.
     * @param config Configuration properties
     * @return GameSystem instance
     */
    public static GameSystem createFromConfig(Properties config) {
        String systemId = config.getProperty("game.system", "dnd");
        return createSystem(systemId);
    }
    
    /**
     * Get all registered system IDs.
     * @return Array of available system identifiers
     */
    public static String[] getAvailableSystems() {
        return REGISTERED_SYSTEMS.keySet().toArray(new String[0]);
    }
    
    /**
     * Check if a system is registered.
     * @param systemId The system identifier
     * @return true if system is available
     */
    public static boolean isSystemAvailable(String systemId) {
        return REGISTERED_SYSTEMS.containsKey(systemId);
    }
}