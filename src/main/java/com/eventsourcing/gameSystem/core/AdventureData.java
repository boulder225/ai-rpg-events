package com.eventsourcing.gameSystem.core;

import java.util.List;
import java.util.Map;

/**
 * Generic adventure data model that works across different game systems.
 * Contains all the information needed to run an adventure in any RPG system.
 */
public record AdventureData(
    String title,
    String description,
    String settingInfo,
    List<String> hooks,
    Map<String, LocationData> locations,
    Map<String, NPCData> npcs,
    List<EncounterData> encounters,
    List<String> treasures,
    Map<String, String> lore
) {
    
    /**
     * Get a location by its ID.
     * @param locationId The location identifier
     * @return LocationData or null if not found
     */
    public LocationData getLocation(String locationId) {
        return locations.get(locationId);
    }
    
    /**
     * Get an NPC by their ID.
     * @param npcId The NPC identifier
     * @return NPCData or null if not found
     */
    public NPCData getNPC(String npcId) {
        return npcs.get(npcId);
    }
    
    /**
     * Get all location IDs.
     * @return Set of all location identifiers
     */
    public java.util.Set<String> getLocationIds() {
        return locations.keySet();
    }
}