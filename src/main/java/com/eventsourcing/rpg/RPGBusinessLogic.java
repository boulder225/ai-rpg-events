package com.eventsourcing.rpg;

import com.eventsourcing.core.domain.CommandResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

/**
 * Business logic for processing RPG commands.
 * Contains the core domain rules and validations.
 * UPDATED: Added proper event sourcing support with CommandResult.
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

    /**
     * Process a generic action command and return events to be persisted.
     * This is the main integration point for event sourcing.
     */
    public static CommandResult<RPGEvent> performAction(RPGState.PlayerState playerState, RPGCommand.PerformAction command) {
        try {
            return switch (command.actionType().toLowerCase()) {
                case "move", "go" -> {
                    String target = command.target();
                    if (target == null || target.isEmpty()) {
                        yield CommandResult.failure("Move command requires a target location");
                    }
                    
                    var moveEvent = new RPGEvent.PlayerMovedToLocation(
                        UUID.randomUUID().toString(),
                        command.playerId(),
                        playerState.currentLocationId(),
                        target,
                        command.issuedAt()
                    );
                    yield CommandResult.success(List.of(moveEvent));
                }
                
                case "attack" -> {
                    String target = command.target();
                    if (target == null || target.isEmpty()) {
                        yield CommandResult.failure("Attack command requires a target");
                    }
                    
                    // Simulate combat outcome
                    String outcome = Math.random() > 0.5 ? "hit" : "miss";
                    var actionEvent = new RPGEvent.ActionPerformed(
                        UUID.randomUUID().toString(),
                        command.playerId(),
                        "attack",
                        target,
                        outcome,
                        command.parameters(),
                        command.issuedAt()
                    );
                    yield CommandResult.success(List.of(actionEvent));
                }
                
                case "search", "examine", "look" -> {
                    String target = command.target() != null ? command.target() : "surroundings";
                    var actionEvent = new RPGEvent.ActionPerformed(
                        UUID.randomUUID().toString(),
                        command.playerId(),
                        command.actionType(),
                        target,
                        "searched",
                        command.parameters(),
                        command.issuedAt()
                    );
                    yield CommandResult.success(List.of(actionEvent));
                }
                
                case "take", "pickup" -> {
                    String target = command.target();
                    if (target == null || target.isEmpty()) {
                        yield CommandResult.failure("Take command requires an item target");
                    }
                    
                    var itemEvent = new RPGEvent.ItemDiscovered(
                        UUID.randomUUID().toString(),
                        command.playerId(),
                        target,
                        "equipment",
                        playerState.currentLocationId(),
                        command.issuedAt()
                    );
                    
                    var actionEvent = new RPGEvent.ActionPerformed(
                        UUID.randomUUID().toString(),
                        command.playerId(),
                        "take",
                        target,
                        "taken",
                        command.parameters(),
                        command.issuedAt()
                    );
                    
                    yield CommandResult.success(List.of(itemEvent, actionEvent));
                }
                
                case "talk", "speak" -> {
                    String target = command.target();
                    if (target == null || target.isEmpty()) {
                        yield CommandResult.failure("Talk command requires an NPC target");
                    }
                    
                    var conversationEvent = new RPGEvent.ConversationOccurred(
                        UUID.randomUUID().toString(),
                        command.playerId(),
                        target,
                        command.parameters().getOrDefault("topic", "general"),
                        "started",
                        command.issuedAt()
                    );
                    yield CommandResult.success(List.of(conversationEvent));
                }
                
                case "rest", "sleep" -> {
                    // Heal player to full health
                    var healthEvent = new RPGEvent.PlayerHealthChanged(
                        UUID.randomUUID().toString(),
                        command.playerId(),
                        playerState.health(),
                        Math.min(100, playerState.health() + 20), // Heal 20 HP
                        "rest",
                        command.issuedAt()
                    );
                    
                    var actionEvent = new RPGEvent.ActionPerformed(
                        UUID.randomUUID().toString(),
                        command.playerId(),
                        "rest",
                        "self",
                        "rested",
                        command.parameters(),
                        command.issuedAt()
                    );
                    
                    yield CommandResult.success(List.of(healthEvent, actionEvent));
                }
                
                default -> {
                    // Generic action
                    var actionEvent = new RPGEvent.ActionPerformed(
                        UUID.randomUUID().toString(),
                        command.playerId(),
                        command.actionType(),
                        command.target() != null ? command.target() : "unknown",
                        "attempted",
                        command.parameters(),
                        command.issuedAt()
                    );
                    yield CommandResult.success(List.of(actionEvent));
                }
            };
            
        } catch (Exception e) {
            return CommandResult.failure("Failed to process action: " + e.getMessage());
        }
    }
    
    // Add more business logic as needed for NPCs, locations, etc.
}
