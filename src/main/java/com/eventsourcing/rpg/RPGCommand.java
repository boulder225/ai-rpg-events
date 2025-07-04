package com.eventsourcing.rpg;

import com.eventsourcing.core.domain.Command;
import java.time.Instant;
import java.util.Map;

/**
 * Commands for the AI-RPG platform.
 * These represent intentions to change the game state.
 */
public sealed interface RPGCommand extends Command {
    
    // Player Commands
    record CreatePlayer(String commandId, String playerId, String name, Instant issuedAt) implements RPGCommand {}
    
    record MovePlayer(String commandId, String playerId, String toLocationId, Instant issuedAt) implements RPGCommand {}
    
    record PerformAction(String commandId, String playerId, String actionType, 
                       String target, Map<String, String> parameters, Instant issuedAt) implements RPGCommand {}
    
    record StartQuest(String commandId, String playerId, String questId, 
                    String giver, Instant issuedAt) implements RPGCommand {}
    
    // NPC Commands
    record CreateNPC(String commandId, String npcId, String name, String type, 
                   String locationId, Instant issuedAt) implements RPGCommand {}
    
    record NPCLearnSkill(String commandId, String npcId, String skillName, 
                       String teacher, Instant issuedAt) implements RPGCommand {}
    
    record NPCSetGoal(String commandId, String npcId, String goal, 
                    String reason, Instant issuedAt) implements RPGCommand {}
    
    // Social Commands
    record InitiateConversation(String commandId, String playerId, String npcId, 
                              String topic, Instant issuedAt) implements RPGCommand {}
    
    record FormRelationship(String commandId, String playerId, String npcId, 
                          String relationType, Instant issuedAt) implements RPGCommand {}
    
    // World Commands
    record DiscoverLocation(String commandId, String playerId, String locationId, 
                          String locationType, Instant issuedAt) implements RPGCommand {}
    
    record TriggerWorldEvent(String commandId, String eventType, String description, 
                           Instant issuedAt) implements RPGCommand {}
}
