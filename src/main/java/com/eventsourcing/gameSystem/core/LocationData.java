package com.eventsourcing.gameSystem.core;

import java.util.List;
import java.util.Map;

/**
 * Generic location data model for any RPG system.
 * Represents a place in the game world with connections to other locations.
 */
public record LocationData(
    String id,
    String name,
    String description,
    String type,
    List<String> features,
    List<String> connections,
    Map<String, String> secrets,
    String icon
) {
    
    /**
     * Check if this location connects to another location.
     * @param locationId The target location ID
     * @return true if connected, false otherwise
     */
    public boolean connectsTo(String locationId) {
        return connections.contains(locationId);
    }
    
    /**
     * Get a formatted display name with icon.
     * @return Icon + name for UI display
     */
    public String getDisplayName() {
        return icon + " " + name;
    }
    
    /**
     * Check if this is a starting location type.
     * @return true if this is typically a starting area
     */
    public boolean isStartingLocation() {
        return type.equals("town") || type.equals("village") || type.equals("home");
    }
}