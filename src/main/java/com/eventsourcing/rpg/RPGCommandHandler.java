package com.eventsourcing.rpg;

import com.eventsourcing.core.domain.CommandResult;
import com.eventsourcing.core.infrastructure.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/**
 * Command handler using functional approach for RPG domain.
 * Handles state reconstruction and command processing.
 */
public class RPGCommandHandler {
    
    private final EventStore<RPGEvent> eventStore;
    
    public RPGCommandHandler(EventStore<RPGEvent> eventStore) {
        this.eventStore = eventStore;
    }
    
    /**
     * Execute a command against a player stream.
     */
    public void executePlayerCommand(String playerId, Function<RPGState.PlayerState, CommandResult<RPGEvent>> commandProcessor) {
        var streamId = StreamId.player(playerId);
        executeCommand(streamId, events -> {
            var playerState = buildPlayerState(playerId, events);
            return commandProcessor.apply(playerState);
        });
    }
    
    /**
     * Execute a command against an NPC stream.
     */
    public void executeNPCCommand(String npcId, Function<RPGState.NPCState, CommandResult<RPGEvent>> commandProcessor) {
        var streamId = StreamId.npc(npcId);
        executeCommand(streamId, events -> {
            var npcState = buildNPCState(npcId, events);
            return commandProcessor.apply(npcState);
        });
    }
    
    /**
     * Execute a command against a location stream.
     */
    public void executeLocationCommand(String locationId, Function<RPGState.LocationState, CommandResult<RPGEvent>> commandProcessor) {
        var streamId = StreamId.location(locationId);
        executeCommand(streamId, events -> {
            var locationState = buildLocationState(locationId, events);
            return commandProcessor.apply(locationState);
        });
    }
    
    /**
     * Query current player state.
     */
    public RPGState.PlayerState getPlayerState(String playerId) {
        var events = eventStore.readStream(StreamId.player(playerId));
        return buildPlayerState(playerId, events);
    }
    
    /**
     * Query current NPC state.
     */
    public RPGState.NPCState getNPCState(String npcId) {
        var events = eventStore.readStream(StreamId.npc(npcId));
        return buildNPCState(npcId, events);
    }
    
    /**
     * Query player state at a specific point in time.
     */
    public RPGState.PlayerState getPlayerStateAt(String playerId, Instant pointInTime) {
        var events = eventStore.readStreamUntil(StreamId.player(playerId), pointInTime);
        return buildPlayerState(playerId, events);
    }
    
    private void executeCommand(StreamId streamId, Function<List<StoredEvent<RPGEvent>>, CommandResult<RPGEvent>> commandProcessor) {
        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                var events = eventStore.readStream(streamId);
                var result = commandProcessor.apply(events);
                
                switch (result) {
                    case CommandResult.Success<RPGEvent> success -> {
                        var appendResult = eventStore.appendToStream(
                            streamId,
                            ExpectedVersion.exact(EventSourcing.getVersion(events)),
                            success.events()
                        );
                        
                        switch (appendResult) {
                            case AppendResult.Success<RPGEvent> appendSuccess -> { return; }
                            case AppendResult.ConcurrentModification<RPGEvent> conflict -> {
                                if (attempt == maxRetries - 1) {
                                    throw new RuntimeException("Max retries exceeded for stream: " + streamId);
                                }
                                continue;
                            }
                        }
                    }
                    case CommandResult.Failure<RPGEvent> failure -> {
                        throw new IllegalArgumentException("Command failed: " + failure.reason());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Command execution failed for stream: " + streamId, e);
            }
        }
    }
    
    private RPGState.PlayerState buildPlayerState(String playerId, List<StoredEvent<RPGEvent>> events) {
        var name = "";
        var currentLocationId = "";
        var health = 100;
        var skills = new HashMap<String, Integer>();
        var relationships = new HashMap<String, RPGState.Relationship>();
        var completedQuests = new ArrayList<String>();
        var activeQuests = new ArrayList<String>();
        var actionHistory = new ArrayList<RPGState.ActionHistory>();
        var lastSeen = Instant.now();
        
        for (var storedEvent : events) {
            var event = storedEvent.event();
            lastSeen = event.occurredAt();
            
            switch (event) {
                case RPGEvent.PlayerCreated created -> {
                    name = created.name();
                }
                case RPGEvent.PlayerMovedToLocation moved -> {
                    currentLocationId = moved.toLocationId();
                }
                case RPGEvent.PlayerHealthChanged healthChanged -> {
                    health = healthChanged.newHealth();
                }
                case RPGEvent.PlayerSkillGained skillGained -> {
                    skills.put(skillGained.skillName(), skillGained.level());
                }
                case RPGEvent.RelationshipFormed formed -> {
                    var relationship = new RPGState.Relationship(
                        formed.npcId(), formed.relationType(), 50, 50, 
                        new ArrayList<>(), formed.occurredAt()
                    );
                    relationships.put(formed.npcId(), relationship);
                }
                case RPGEvent.RelationshipChanged changed -> {
                    var existing = relationships.get(changed.npcId());
                    if (existing != null) {
                        var updated = new RPGState.Relationship(
                            existing.entityId(),
                            changed.newType(),
                            existing.trustLevel() + changed.trustChange(),
                            existing.friendshipLevel(),
                            existing.sharedExperiences(),
                            changed.occurredAt()
                        );
                        relationships.put(changed.npcId(), updated);
                    }
                }
                case RPGEvent.QuestStarted questStarted -> {
                    activeQuests.add(questStarted.questId());
                }
                case RPGEvent.QuestCompleted questCompleted -> {
                    activeQuests.remove(questCompleted.questId());
                    completedQuests.add(questCompleted.questId());
                }
                case RPGEvent.ActionPerformed actionPerformed -> {
                    var action = new RPGState.ActionHistory(
                        actionPerformed.actionType(),
                        actionPerformed.target(),
                        actionPerformed.outcome(),
                        actionPerformed.context().getOrDefault("locationId", ""),
                        actionPerformed.occurredAt()
                    );
                    actionHistory.add(action);
                }
                default -> {
                    // Ignore events not relevant to player state
                }
            }
        }
        
        return new RPGState.PlayerState(
            playerId, name, currentLocationId, health, skills,
            relationships, completedQuests, activeQuests, actionHistory, lastSeen
        );
    }
    
    private RPGState.NPCState buildNPCState(String npcId, List<StoredEvent<RPGEvent>> events) {
        var name = "";
        var type = "";
        var currentLocationId = "";
        var skills = new HashMap<String, Integer>();
        var currentGoal = "";
        var relationships = new HashMap<String, RPGState.Relationship>();
        var knownPlayers = new ArrayList<String>();
        var lastActivity = Instant.now();
        
        for (var storedEvent : events) {
            var event = storedEvent.event();
            lastActivity = event.occurredAt();
            
            switch (event) {
                case RPGEvent.NPCCreated created -> {
                    name = created.name();
                    type = created.type();
                    currentLocationId = created.locationId();
                }
                case RPGEvent.NPCSkillLearned skillLearned -> {
                    skills.put(skillLearned.skillName(), skillLearned.level());
                }
                case RPGEvent.NPCGoalChanged goalChanged -> {
                    currentGoal = goalChanged.newGoal();
                }
                case RPGEvent.NPCMovedToLocation moved -> {
                    currentLocationId = moved.toLocationId();
                }
                case RPGEvent.RelationshipFormed formed -> {
                    if (formed.npcId().equals(npcId)) {
                        var relationship = new RPGState.Relationship(
                            formed.playerId(), formed.relationType(), 50, 50,
                            new ArrayList<>(), formed.occurredAt()
                        );
                        relationships.put(formed.playerId(), relationship);
                        knownPlayers.add(formed.playerId());
                    }
                }
                case RPGEvent.ConversationOccurred conversation -> {
                    if (conversation.npcId().equals(npcId) && !knownPlayers.contains(conversation.playerId())) {
                        knownPlayers.add(conversation.playerId());
                    }
                }
                default -> {
                    // Ignore events not relevant to NPC state
                }
            }
        }
        
        return new RPGState.NPCState(
            npcId, name, type, currentLocationId, skills,
            currentGoal, relationships, knownPlayers, lastActivity
        );
    }
    
    private RPGState.LocationState buildLocationState(String locationId, List<StoredEvent<RPGEvent>> events) {
        var type = "";
        var properties = new HashMap<String, String>();
        var visitedBy = new ArrayList<String>();
        var timeSpentBy = new HashMap<String, Integer>();
        var itemsFound = new ArrayList<String>();
        var currentOccupants = new ArrayList<String>();
        var lastModified = Instant.now();
        
        for (var storedEvent : events) {
            var event = storedEvent.event();
            lastModified = event.occurredAt();
            
            switch (event) {
                case RPGEvent.LocationDiscovered discovered -> {
                    type = discovered.locationType();
                    properties.putAll(discovered.properties());
                    if (!visitedBy.contains(discovered.playerId())) {
                        visitedBy.add(discovered.playerId());
                    }
                }
                case RPGEvent.PlayerMovedToLocation moved -> {
                    if (moved.toLocationId().equals(locationId)) {
                        if (!currentOccupants.contains(moved.playerId())) {
                            currentOccupants.add(moved.playerId());
                        }
                    } else if (moved.fromLocationId().equals(locationId)) {
                        currentOccupants.remove(moved.playerId());
                    }
                }
                case RPGEvent.ItemDiscovered itemDiscovered -> {
                    if (itemDiscovered.locationId().equals(locationId)) {
                        itemsFound.add(itemDiscovered.itemId());
                    }
                }
                case RPGEvent.LocationChanged changed -> {
                    properties.put("lastChange", changed.description());
                    properties.put("changedBy", changed.causedBy());
                }
                default -> {
                    // Ignore events not relevant to location state
                }
            }
        }
        
        return new RPGState.LocationState(
            locationId, type, properties, visitedBy,
            timeSpentBy, itemsFound, currentOccupants, lastModified
        );
    }
}
