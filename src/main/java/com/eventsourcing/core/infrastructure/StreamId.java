package com.eventsourcing.core.infrastructure;

/**
 * Unique identifier for an event stream.
 * Combines context and identifier for proper stream separation.
 */
public record StreamId(String value) {
    public static StreamId of(String context, String identifier) {
        return new StreamId(context + "-" + identifier);
    }
    
    public static StreamId player(String playerId) {
        return of("player", playerId);
    }
    
    public static StreamId npc(String npcId) {
        return of("npc", npcId);
    }
    
    public static StreamId location(String locationId) {
        return of("location", locationId);
    }
    
    // Alternative naming for clarity
    public static StreamId forPlayer(String playerId) {
        return player(playerId);
    }
    
    public static StreamId forNPC(String npcId) {
        return npc(npcId);
    }
    
    public static StreamId forLocation(String locationId) {
        return location(locationId);
    }
}
