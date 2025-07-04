package com.eventsourcing.rpg;

import com.eventsourcing.core.domain.CommandResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Business logic for processing RPG commands.
 * Contains the core domain rules and validations.
 */
public class RPGBusinessLogic {
    
    /**
     * Process player creation command.
     */
    public static CommandResult<RPGEvent> createPlayer(RPGCommand.CreatePlayer command) {
        var event = new RPGEvent.PlayerCreated(
            UUID.randomUUID().toString(),
            command.playerId(),
            command.name(),
            command.issuedAt()
        );
        return new CommandResult.Success<>(List.of(event));
    }
    
    /**
     * Process player movement command.
     */
    public static CommandResult<RPGEvent> movePlayer(RPGState.PlayerState currentState, RPGCommand.MovePlayer command) {
        if (currentState.currentLocationId().equals(command.toLocationId())) {
            return new CommandResult.Failure<>("Player already at location: " + command.toLocationId());
        }
        
        var event = new RPGEvent.PlayerMovedToLocation(
            UUID.randomUUID().toString(),
            command.playerId(),
            currentState.currentLocationId(),
            command.toLocationId(),
            command.issuedAt()
        );
        return new CommandResult.Success<>(List.of(event));
    }
    
    /**
     * Process action performance command.
     */
    public static CommandResult<RPGEvent> performAction(RPGState.PlayerState currentState, RPGCommand.PerformAction command) {
        // Basic validation
        if (currentState.health() <= 0) {
            return new CommandResult.Failure<>("Player is incapacitated");
        }
        
        // Simulate action outcome
        var outcome = determineActionOutcome(command.actionType(), command.target(), currentState);
        
        var event = new RPGEvent.ActionPerformed(
            UUID.randomUUID().toString(),
            command.playerId(),
            command.actionType(),
            command.target(),
            outcome,
            command.parameters(),
            command.issuedAt()
        );
        
        return new CommandResult.Success<>(List.of(event));
    }
    
    /**
     * Process NPC creation command.
     */
    public static CommandResult<RPGEvent> createNPC(RPGCommand.CreateNPC command) {
        var event = new RPGEvent.NPCCreated(
            UUID.randomUUID().toString(),
            command.npcId(),
            command.name(),
            command.type(),
            command.locationId(),
            command.issuedAt()
        );
        return new CommandResult.Success<>(List.of(event));
    }
    
    /**
     * Process NPC skill learning command.
     */
    public static CommandResult<RPGEvent> npcLearnSkill(RPGState.NPCState currentState, RPGCommand.NPCLearnSkill command) {
        var currentLevel = currentState.skills().getOrDefault(command.skillName(), 0);
        var newLevel = currentLevel + 1;
        
        var event = new RPGEvent.NPCSkillLearned(
            UUID.randomUUID().toString(),
            command.npcId(),
            command.skillName(),
            newLevel,
            command.teacher(),
            command.issuedAt()
        );
        
        return new CommandResult.Success<>(List.of(event));
    }
    
    /**
     * Process conversation initiation command.
     */
    public static CommandResult<RPGEvent> initiateConversation(RPGState.PlayerState playerState, RPGCommand.InitiateConversation command) {
        // Simulate conversation outcome
        var outcome = "friendly";
        if (playerState.relationships().containsKey(command.npcId())) {
            var relationship = playerState.relationships().get(command.npcId());
            if (relationship.trustLevel() < 25) {
                outcome = "suspicious";
            } else if (relationship.trustLevel() > 75) {
                outcome = "warm";
            }
        }
        
        var conversationEvent = new RPGEvent.ConversationOccurred(
            UUID.randomUUID().toString(),
            command.playerId(),
            command.npcId(),
            command.topic(),
            outcome,
            command.issuedAt()
        );
        
        // If no relationship exists, form one
        if (!playerState.relationships().containsKey(command.npcId())) {
            var relationshipEvent = new RPGEvent.RelationshipFormed(
                UUID.randomUUID().toString(),
                command.playerId(),
                command.npcId(),
                "acquaintance",
                command.issuedAt()
            );
            return new CommandResult.Success<>(List.of(conversationEvent, relationshipEvent));
        }
        
        return new CommandResult.Success<>(List.of(conversationEvent));
    }
    
    /**
     * Process location discovery command.
     */
    public static CommandResult<RPGEvent> discoverLocation(RPGCommand.DiscoverLocation command) {
        var event = new RPGEvent.LocationDiscovered(
            UUID.randomUUID().toString(),
            command.playerId(),
            command.locationId(),
            command.locationType(),
            Map.of("discoveredAt", command.issuedAt().toString()),
            command.issuedAt()
        );
        
        return new CommandResult.Success<>(List.of(event));
    }
    
    /**
     * Process quest start command.
     */
    public static CommandResult<RPGEvent> startQuest(RPGState.PlayerState playerState, RPGCommand.StartQuest command) {
        if (playerState.activeQuests().contains(command.questId())) {
            return new CommandResult.Failure<>("Quest already active: " + command.questId());
        }
        
        if (playerState.completedQuests().contains(command.questId())) {
            return new CommandResult.Failure<>("Quest already completed: " + command.questId());
        }
        
        var event = new RPGEvent.QuestStarted(
            UUID.randomUUID().toString(),
            command.playerId(),
            command.questId(),
            "Quest: " + command.questId(),
            command.giver(),
            command.issuedAt()
        );
        
        return new CommandResult.Success<>(List.of(event));
    }
    
    /**
     * AI-driven NPC goal setting.
     */
    public static CommandResult<RPGEvent> setNPCGoal(RPGState.NPCState currentState, String newGoal, String reason) {
        if (currentState.currentGoal().equals(newGoal)) {
            return new CommandResult.Failure<>("NPC already has this goal: " + newGoal);
        }
        
        var event = new RPGEvent.NPCGoalChanged(
            UUID.randomUUID().toString(),
            currentState.npcId(),
            currentState.currentGoal(),
            newGoal,
            reason,
            Instant.now()
        );
        
        return new CommandResult.Success<>(List.of(event));
    }
    
    /**
     * Trigger autonomous world events.
     */
    public static CommandResult<RPGEvent> triggerWorldEvent(String eventType, String description, List<String> affectedEntities) {
        var event = new RPGEvent.WorldEventTriggered(
            UUID.randomUUID().toString(),
            eventType,
            description,
            affectedEntities,
            Instant.now()
        );
        
        return new CommandResult.Success<>(List.of(event));
    }
    
    private static String determineActionOutcome(String actionType, String target, RPGState.PlayerState playerState) {
        return switch (actionType.toLowerCase()) {
            case "attack" -> {
                var skill = playerState.skills().getOrDefault("combat", 1);
                yield skill > 5 ? "success" : "partial_success";
            }
            case "persuade" -> {
                var skill = playerState.skills().getOrDefault("charisma", 1);
                yield skill > 3 ? "success" : "failure";
            }
            case "explore" -> "discovered_something";
            case "rest" -> "recovered";
            default -> "unknown_outcome";
        };
    }
}
