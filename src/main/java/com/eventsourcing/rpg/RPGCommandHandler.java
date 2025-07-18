package com.eventsourcing.rpg;

import com.eventsourcing.core.domain.CommandResult;
import com.eventsourcing.core.infrastructure.*;
import com.eventsourcing.gameSystem.context.LocationContextManager;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/**
 * Command handler using KISS approach for RPG domain.
 * Uses simple in-memory state management for fast prototyping and easy maintenance.
 */
public class RPGCommandHandler {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RPGCommandHandler.class);
    
    // Simple in-memory state maps (KISS approach)
    private final Map<String, RPGState.PlayerState> playerStateMap = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, RPGState.LocationState> locationStateMap = new java.util.concurrent.ConcurrentHashMap<>();
    private LocationContextManager locationContextManager;
    
    public RPGCommandHandler() {
        // No complex setup needed - KISS!
    }
    
    public void setLocationContextManager(LocationContextManager locationContextManager) {
        this.locationContextManager = locationContextManager;
    }
    
    // --- Player State CRUD ---
    public RPGState.PlayerState getPlayerState(String playerId) {
        return playerStateMap.get(playerId);
    }
    
    public void putPlayerState(String playerId, RPGState.PlayerState state) {
        playerStateMap.put(playerId, state);
    }
    
    public boolean hasPlayer(String playerId) {
        return playerStateMap.containsKey(playerId);
    }
    
    // --- Location State CRUD ---
    public RPGState.LocationState getLocationState(String locationId) {
        return locationStateMap.get(locationId);
    }
    
    public void putLocationState(String locationId, RPGState.LocationState state) {
        locationStateMap.put(locationId, state);
    }
    
    public boolean hasLocation(String locationId) {
        return locationStateMap.containsKey(locationId);
    }
    
    // --- Simple Operations ---
    public void createPlayer(String playerId, String name) {
        RPGState.PlayerState state = new RPGState.PlayerState(
            playerId, name, "village", 100, Map.of(), Map.of(), List.of(), List.of(), List.of(), java.time.Instant.now()
        );
        putPlayerState(playerId, state);
        log.info("[KISS] Created player: {}", playerId);
    }
    
    public void movePlayer(String playerId, String toLocationId) {
        RPGState.PlayerState state = getPlayerState(playerId);
        if (state == null) {
            log.warn("[KISS] Player not found: {}", playerId);
            return;
        }
        
        String fromLocation = state.currentLocationId();
        
        RPGState.PlayerState newState = new RPGState.PlayerState(
            state.playerId(), state.name(), toLocationId, state.health(), state.skills(), state.relationships(),
            state.completedQuests(), state.activeQuests(), state.actionHistory(), java.time.Instant.now()
        );
        putPlayerState(playerId, newState);
        
        // ENHANCEMENT: Trigger location context update (simple notification)
        if (locationContextManager != null) {
            // Create a simple movement event for location context update
            var moveEvent = new RPGEvent.PlayerMovedToLocation(
                java.util.UUID.randomUUID().toString(),
                playerId,
                fromLocation,
                toLocationId,
                java.time.Instant.now()
            );
            locationContextManager.onPlayerMovement(moveEvent);
        }
        
        log.info("[KISS] Player {} moved from {} to {}", playerId, fromLocation, toLocationId);
    }
    
    // Add more simple KISS methods as needed for actions, health, etc.
}
