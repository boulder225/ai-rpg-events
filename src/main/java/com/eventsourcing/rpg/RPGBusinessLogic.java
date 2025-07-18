package com.eventsourcing.rpg;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Business logic for processing RPG commands (KISS version).
 * Contains the core domain rules and validations.
 */
public class RPGBusinessLogic {
    // Create a new player state
    public static RPGState.PlayerState createPlayer(String playerId, String name) {
        return new RPGState.PlayerState(
            playerId,
            name,
            "village",
            100,
            Map.of(),
            Map.of(),
            List.of(),
            List.of(),
            List.of(),
            Instant.now()
        );
    }

    // Move player to a new location
    public static RPGState.PlayerState movePlayer(RPGState.PlayerState state, String toLocationId) {
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            toLocationId,
            state.health(),
            state.skills(),
            state.relationships(),
            state.completedQuests(),
            state.activeQuests(),
            state.actionHistory(),
            Instant.now()
        );
    }

    // Change player health
    public static RPGState.PlayerState changePlayerHealth(RPGState.PlayerState state, int newHealth) {
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            state.currentLocationId(),
            newHealth,
            state.skills(),
            state.relationships(),
            state.completedQuests(),
            state.activeQuests(),
            state.actionHistory(),
            Instant.now()
        );
    }

    // Add a skill to player
    public static RPGState.PlayerState addPlayerSkill(RPGState.PlayerState state, String skillName, int level) {
        var newSkills = new java.util.HashMap<>(state.skills());
        newSkills.put(skillName, level);
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            state.currentLocationId(),
            state.health(),
            newSkills,
            state.relationships(),
            state.completedQuests(),
            state.activeQuests(),
            state.actionHistory(),
            Instant.now()
        );
    }

    // Add a relationship
    public static RPGState.PlayerState addRelationship(RPGState.PlayerState state, String npcId, String relationType) {
        var newRelationships = new java.util.HashMap<>(state.relationships());
        newRelationships.put(npcId, new RPGState.Relationship(
            npcId, relationType, 50, 50, List.of(), Instant.now()
        ));
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            state.currentLocationId(),
            state.health(),
            state.skills(),
            newRelationships,
            state.completedQuests(),
            state.activeQuests(),
            state.actionHistory(),
            Instant.now()
        );
    }

    // Start a quest
    public static RPGState.PlayerState startQuest(RPGState.PlayerState state, String questId) {
        var newActiveQuests = new java.util.ArrayList<>(state.activeQuests());
        if (!newActiveQuests.contains(questId)) newActiveQuests.add(questId);
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            state.currentLocationId(),
            state.health(),
            state.skills(),
            state.relationships(),
            state.completedQuests(),
            newActiveQuests,
            state.actionHistory(),
            Instant.now()
        );
    }

    // Complete a quest
    public static RPGState.PlayerState completeQuest(RPGState.PlayerState state, String questId) {
        var newActiveQuests = new java.util.ArrayList<>(state.activeQuests());
        var newCompletedQuests = new java.util.ArrayList<>(state.completedQuests());
        newActiveQuests.remove(questId);
        if (!newCompletedQuests.contains(questId)) newCompletedQuests.add(questId);
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            state.currentLocationId(),
            state.health(),
            state.skills(),
            state.relationships(),
            newCompletedQuests,
            newActiveQuests,
            state.actionHistory(),
            Instant.now()
        );
    }

    // Add an action to history
    public static RPGState.PlayerState addAction(RPGState.PlayerState state, String actionType, String target, String outcome, String locationId) {
        var newHistory = new java.util.ArrayList<>(state.actionHistory());
        newHistory.add(new RPGState.ActionHistory(
            actionType,
            target,
            outcome,
            locationId,
            Instant.now()
        ));
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            state.currentLocationId(),
            state.health(),
            state.skills(),
            state.relationships(),
            state.completedQuests(),
            state.activeQuests(),
            newHistory,
            Instant.now()
        );
    }

    // Add more KISS business logic as needed for NPCs, locations, etc.
}
