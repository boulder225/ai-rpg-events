package com.eventsourcing.rpg;

import com.eventsourcing.core.domain.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Domain events for the AI-RPG platform.
 * These events capture the persistent narrative context and evolving relationships.
 */
public sealed interface RPGEvent extends DomainEvent {
    
    // Player Journey Events
    record PlayerCreated(String eventId, String playerId, String name, Instant occurredAt) implements RPGEvent {}
    
    record PlayerMovedToLocation(String eventId, String playerId, String fromLocationId, 
                               String toLocationId, Instant occurredAt) implements RPGEvent {}
    
    record PlayerHealthChanged(String eventId, String playerId, int oldHealth, 
                             int newHealth, String reason, Instant occurredAt) implements RPGEvent {}
    
    record PlayerSkillGained(String eventId, String playerId, String skillName, 
                           int level, Instant occurredAt) implements RPGEvent {}
    
    // Social Network Events
    record RelationshipFormed(String eventId, String playerId, String npcId, 
                            String relationType, Instant occurredAt) implements RPGEvent {}
    
    record RelationshipChanged(String eventId, String playerId, String npcId, 
                             String oldType, String newType, int trustChange, 
                             String reason, Instant occurredAt) implements RPGEvent {}
    
    record ConversationOccurred(String eventId, String playerId, String npcId, 
                              String topic, String outcome, Instant occurredAt) implements RPGEvent {}
    
    // NPC Evolution Events
    record NPCCreated(String eventId, String npcId, String name, String type, 
                     String locationId, Instant occurredAt) implements RPGEvent {}
    
    record NPCSkillLearned(String eventId, String npcId, String skillName, 
                         int level, String learnedFrom, Instant occurredAt) implements RPGEvent {}
    
    record NPCGoalChanged(String eventId, String npcId, String oldGoal, 
                        String newGoal, String reason, Instant occurredAt) implements RPGEvent {}
    
    record NPCMovedToLocation(String eventId, String npcId, String fromLocationId, 
                            String toLocationId, String purpose, Instant occurredAt) implements RPGEvent {}
    
    // World State Events
    record LocationDiscovered(String eventId, String playerId, String locationId, 
                            String locationType, Map<String, String> properties, 
                            Instant occurredAt) implements RPGEvent {}
    
    record LocationChanged(String eventId, String locationId, String changeType, 
                         String description, String causedBy, Instant occurredAt) implements RPGEvent {}
    
    record ItemDiscovered(String eventId, String playerId, String itemId, 
                        String itemType, String locationId, Instant occurredAt) implements RPGEvent {}
    
    // Action Events
    record ActionPerformed(String eventId, String playerId, String actionType, 
                         String target, String outcome, Map<String, String> context, 
                         Instant occurredAt) implements RPGEvent {}
    
    record QuestStarted(String eventId, String playerId, String questId, 
                      String questName, String giver, Instant occurredAt) implements RPGEvent {}
    
    record QuestCompleted(String eventId, String playerId, String questId, 
                        String outcome, List<String> rewards, Instant occurredAt) implements RPGEvent {}
    
    // Autonomous Agent Events
    record AIDecisionMade(String eventId, String agentId, String agentType, 
                        String decision, String reasoning, Map<String, String> context, 
                        Instant occurredAt) implements RPGEvent {}
    
    record WorldEventTriggered(String eventId, String eventType, String description, 
                             List<String> affectedEntities, Instant occurredAt) implements RPGEvent {}
}
