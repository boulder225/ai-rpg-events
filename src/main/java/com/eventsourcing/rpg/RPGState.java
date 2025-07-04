package com.eventsourcing.rpg;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Read models for querying the current state of the RPG world.
 * These are rebuilt from events and represent the current "living" state.
 */
public class RPGState {
    
    /**
     * Complete player context including narrative history and current state.
     */
    public record PlayerState(
        String playerId,
        String name,
        String currentLocationId,
        int health,
        Map<String, Integer> skills,
        Map<String, Relationship> relationships,
        List<String> completedQuests,
        List<String> activeQuests,
        List<ActionHistory> actionHistory,
        Instant lastSeen
    ) {}
    
    /**
     * NPC state with autonomous evolution tracking.
     */
    public record NPCState(
        String npcId,
        String name,
        String type,
        String currentLocationId,
        Map<String, Integer> skills,
        String currentGoal,
        Map<String, Relationship> relationships,
        List<String> knownPlayers,
        Instant lastActivity
    ) {}
    
    /**
     * Location state with persistent memory of player interactions.
     */
    public record LocationState(
        String locationId,
        String type,
        Map<String, String> properties,
        List<String> visitedBy,
        Map<String, Integer> timeSpentBy,
        List<String> itemsFound,
        List<String> currentOccupants,
        Instant lastModified
    ) {}
    
    /**
     * Relationship tracking between entities.
     */
    public record Relationship(
        String entityId,
        String relationType,
        int trustLevel,
        int friendshipLevel,
        List<String> sharedExperiences,
        Instant lastInteraction
    ) {}
    
    /**
     * Action history for narrative context.
     */
    public record ActionHistory(
        String actionType,
        String target,
        String outcome,
        String locationId,
        Instant timestamp
    ) {}
    
    /**
     * World event tracking for autonomous agent context.
     */
    public record WorldEvent(
        String eventType,
        String description,
        List<String> affectedEntities,
        Instant occurredAt
    ) {}
}
