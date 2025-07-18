package com.eventsourcing.rpg;

import com.eventsourcing.core.domain.CommandResult;
import com.eventsourcing.core.infrastructure.*;
import com.eventsourcing.gameSystem.context.LocationContextManager;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/**
 * Event-sourced command handler for RPG domain.
 * Implements proper event sourcing patterns with state reconstruction and optimistic concurrency.
 * 
 * FIXED: Now properly implements event sourcing instead of CRUD operations.
 */
public class RPGCommandHandler {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RPGCommandHandler.class);
    
    private final EventStore<RPGEvent> eventStore;
    private LocationContextManager locationContextManager;
    
    // State caches for performance (rebuilt from events)
    private final Map<String, RPGState.PlayerState> playerStateCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, RPGState.LocationState> locationStateCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Instant> cacheTimestamps = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 30_000; // 30 seconds
    
    public RPGCommandHandler(EventStore<RPGEvent> eventStore) {
        this.eventStore = eventStore;
        log.info("Initialized RPGCommandHandler with proper event sourcing");
    }
    
    // Backward compatibility constructor (creates in-memory event store)
    public RPGCommandHandler() {
        this(new InMemoryEventStore<>());
        log.warn("Using in-memory event store - consider using persistent storage for production");
    }
    
    public void setLocationContextManager(LocationContextManager locationContextManager) {
        this.locationContextManager = locationContextManager;
    }
    
    /**
     * Execute a command using proper event sourcing pattern
     */
    public CommandResult<RPGEvent> executeCommand(RPGCommand command) {
        try {
            return switch (command) {
                case RPGCommand.CreatePlayer createCmd -> handleCreatePlayer(createCmd);
                case RPGCommand.MovePlayer moveCmd -> handleMovePlayer(moveCmd);
                case RPGCommand.PerformAction actionCmd -> handlePerformAction(actionCmd);
                case RPGCommand.StartQuest questCmd -> handleStartQuest(questCmd);
                case RPGCommand.CreateNPC npcCmd -> handleCreateNPC(npcCmd);
                case RPGCommand.InitiateConversation convCmd -> handleInitiateConversation(convCmd);
                default -> CommandResult.failure("Unknown command type: " + command.getClass().getSimpleName());
            };
        } catch (Exception e) {
            log.error("Command execution failed: {}", e.getMessage(), e);
            return CommandResult.failure("Command execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Get current player state (rebuilt from events or cached)
     */
    public RPGState.PlayerState getPlayerState(String playerId) {
        // Check cache first
        var cached = getCachedPlayerState(playerId);
        if (cached != null) {
            return cached;
        }
        
        // Rebuild from events
        var streamId = StreamId.forPlayer(playerId);
        var events = eventStore.readStream(streamId);
        
        if (events.isEmpty()) {
            return null; // Player doesn't exist
        }
        
        var state = rebuildPlayerState(events);
        cachePlayerState(playerId, state);
        return state;
    }
    
    /**
     * Get current location state (rebuilt from events or cached)
     */
    public RPGState.LocationState getLocationState(String locationId) {
        // Check cache first
        var cached = getCachedLocationState(locationId);
        if (cached != null) {
            return cached;
        }
        
        // Rebuild from events
        var streamId = StreamId.forLocation(locationId);
        var events = eventStore.readStream(streamId);
        
        var state = rebuildLocationState(locationId, events);
        cacheLocationState(locationId, state);
        return state;
    }
    
    // Convenience methods for backward compatibility
    public void createPlayer(String playerId, String name) {
        var command = new RPGCommand.CreatePlayer(
            UUID.randomUUID().toString(), playerId, name, Instant.now()
        );
        var result = executeCommand(command);
        if (result instanceof CommandResult.Failure<RPGEvent> failure) {
            log.error("Failed to create player {}: {}", playerId, failure.reason());
        }
    }
    
    public void movePlayer(String playerId, String toLocationId) {
        var command = new RPGCommand.MovePlayer(
            UUID.randomUUID().toString(), playerId, toLocationId, Instant.now()
        );
        var result = executeCommand(command);
        if (result instanceof CommandResult.Failure<RPGEvent> failure) {
            log.error("Failed to move player {}: {}", playerId, failure.reason());
        }
    }
    
    public void putPlayerState(String playerId, RPGState.PlayerState state) {
        // For backward compatibility - cache the state but warn about direct mutation
        log.warn("Direct state mutation detected for player {}. Consider using commands instead.", playerId);
        cachePlayerState(playerId, state);
    }
    
    public void putLocationState(String locationId, RPGState.LocationState state) {
        // For backward compatibility - cache the state but warn about direct mutation
        log.warn("Direct state mutation detected for location {}. Consider using events instead.", locationId);
        cacheLocationState(locationId, state);
    }
    
    public boolean hasPlayer(String playerId) {
        return getPlayerState(playerId) != null;
    }
    
    public boolean hasLocation(String locationId) {
        return getLocationState(locationId) != null;
    }
    
    // === Command Handlers ===
    
    private CommandResult<RPGEvent> handleCreatePlayer(RPGCommand.CreatePlayer command) {
        var streamId = StreamId.forPlayer(command.playerId());
        var existingEvents = eventStore.readStream(streamId);
        
        if (!existingEvents.isEmpty()) {
            return CommandResult.failure("Player " + command.playerId() + " already exists");
        }
        
        var event = new RPGEvent.PlayerCreated(
            UUID.randomUUID().toString(),
            command.playerId(),
            command.name(),
            command.issuedAt()
        );
        
        var appendResult = eventStore.appendToStream(
            streamId, 
            ExpectedVersion.noStream(), 
            List.of(event)
        );
        
        return switch (appendResult) {
            case AppendResult.Success<RPGEvent> success -> {
                invalidatePlayerCache(command.playerId());
                log.info("Player created: {}", command.playerId());
                yield CommandResult.success(success.events().stream().map(StoredEvent::event).toList());
            }
            case AppendResult.ConcurrentModification<RPGEvent> conflict -> {
                yield CommandResult.failure("Concurrent modification detected");
            }
        };
    }
    
    private CommandResult<RPGEvent> handleMovePlayer(RPGCommand.MovePlayer command) {
        var streamId = StreamId.forPlayer(command.playerId());
        var existingEvents = eventStore.readStream(streamId);
        
        if (existingEvents.isEmpty()) {
            return CommandResult.failure("Player " + command.playerId() + " not found");
        }
        
        var currentState = rebuildPlayerState(existingEvents);
        var fromLocation = currentState.currentLocationId();
        
        if (fromLocation.equals(command.toLocationId())) {
            return CommandResult.failure("Player is already at " + command.toLocationId());
        }
        
        var event = new RPGEvent.PlayerMovedToLocation(
            UUID.randomUUID().toString(),
            command.playerId(),
            fromLocation,
            command.toLocationId(),
            command.issuedAt()
        );
        
        var expectedVersion = ExpectedVersion.exact(existingEvents.size() - 1);
        var appendResult = eventStore.appendToStream(streamId, expectedVersion, List.of(event));
        
        return switch (appendResult) {
            case AppendResult.Success<RPGEvent> success -> {
                invalidatePlayerCache(command.playerId());
                
                // Trigger location context update
                if (locationContextManager != null) {
                    locationContextManager.onPlayerMovement((RPGEvent.PlayerMovedToLocation) event);
                }
                
                log.info("Player {} moved from {} to {}", command.playerId(), fromLocation, command.toLocationId());
                yield CommandResult.success(success.events().stream().map(StoredEvent::event).toList());
            }
            case AppendResult.ConcurrentModification<RPGEvent> conflict -> {
                yield CommandResult.failure("Concurrent modification detected - please retry");
            }
        };
    }
    
    private CommandResult<RPGEvent> handlePerformAction(RPGCommand.PerformAction command) {
        var streamId = StreamId.forPlayer(command.playerId());
        var existingEvents = eventStore.readStream(streamId);
        
        if (existingEvents.isEmpty()) {
            return CommandResult.failure("Player " + command.playerId() + " not found");
        }
        
        // Use business logic to determine outcome
        var currentState = rebuildPlayerState(existingEvents);
        var businessResult = RPGBusinessLogic.performAction(currentState, command);
        
        if (businessResult instanceof CommandResult.Failure<RPGEvent> failure) {
            return failure;
        }
        
        var success = (CommandResult.Success<RPGEvent>) businessResult;
        var expectedVersion = ExpectedVersion.exact(existingEvents.size() - 1);
        var appendResult = eventStore.appendToStream(streamId, expectedVersion, success.events());
        
        return switch (appendResult) {
            case AppendResult.Success<RPGEvent> appendSuccess -> {
                invalidatePlayerCache(command.playerId());
                log.info("Action performed: {} by {}", command.actionType(), command.playerId());
                yield CommandResult.success(appendSuccess.events().stream().map(StoredEvent::event).toList());
            }
            case AppendResult.ConcurrentModification<RPGEvent> conflict -> {
                yield CommandResult.failure("Concurrent modification detected - please retry");
            }
        };
    }
    
    private CommandResult<RPGEvent> handleStartQuest(RPGCommand.StartQuest command) {
        var event = new RPGEvent.QuestStarted(
            UUID.randomUUID().toString(),
            command.playerId(),
            command.questId(),
            command.questId(), // questName - would come from quest data
            command.giver(),
            command.issuedAt()
        );
        
        var streamId = StreamId.forPlayer(command.playerId());
        var existingEvents = eventStore.readStream(streamId);
        var expectedVersion = existingEvents.isEmpty() ? 
            ExpectedVersion.noStream() : 
            ExpectedVersion.exact(existingEvents.size() - 1);
            
        var appendResult = eventStore.appendToStream(streamId, expectedVersion, List.of(event));
        
        return switch (appendResult) {
            case AppendResult.Success<RPGEvent> success -> {
                invalidatePlayerCache(command.playerId());
                yield CommandResult.success(success.events().stream().map(StoredEvent::event).toList());
            }
            case AppendResult.ConcurrentModification<RPGEvent> conflict -> {
                yield CommandResult.failure("Concurrent modification detected");
            }
        };
    }
    
    private CommandResult<RPGEvent> handleCreateNPC(RPGCommand.CreateNPC command) {
        var event = new RPGEvent.NPCCreated(
            UUID.randomUUID().toString(),
            command.npcId(),
            command.name(),
            command.type(),
            command.locationId(),
            command.issuedAt()
        );
        
        var streamId = StreamId.forNPC(command.npcId());
        var appendResult = eventStore.appendToStream(
            streamId, 
            ExpectedVersion.noStream(), 
            List.of(event)
        );
        
        return switch (appendResult) {
            case AppendResult.Success<RPGEvent> success -> {
                yield CommandResult.success(success.events().stream().map(StoredEvent::event).toList());
            }
            case AppendResult.ConcurrentModification<RPGEvent> conflict -> {
                yield CommandResult.failure("NPC already exists");
            }
        };
    }
    
    private CommandResult<RPGEvent> handleInitiateConversation(RPGCommand.InitiateConversation command) {
        var event = new RPGEvent.ConversationOccurred(
            UUID.randomUUID().toString(),
            command.playerId(),
            command.npcId(),
            command.topic(),
            "ongoing", // outcome determined by AI
            command.issuedAt()
        );
        
        var streamId = StreamId.forPlayer(command.playerId());
        var existingEvents = eventStore.readStream(streamId);
        var expectedVersion = existingEvents.isEmpty() ? 
            ExpectedVersion.noStream() : 
            ExpectedVersion.exact(existingEvents.size() - 1);
            
        var appendResult = eventStore.appendToStream(streamId, expectedVersion, List.of(event));
        
        return switch (appendResult) {
            case AppendResult.Success<RPGEvent> success -> {
                invalidatePlayerCache(command.playerId());
                yield CommandResult.success(success.events().stream().map(StoredEvent::event).toList());
            }
            case AppendResult.ConcurrentModification<RPGEvent> conflict -> {
                yield CommandResult.failure("Concurrent modification detected");
            }
        };
    }
    
    // === State Rebuilding ===
    
    private RPGState.PlayerState rebuildPlayerState(List<StoredEvent<RPGEvent>> events) {
        var initialState = new RPGState.PlayerState(
            "", "", "village", 100, Map.of(), Map.of(), 
            List.of(), List.of(), List.of(), Instant.now()
        );
        
        return EventSourcing.fromEvents(initialState, events, this::applyEventToPlayerState);
    }
    
    private RPGState.LocationState rebuildLocationState(String locationId, List<StoredEvent<RPGEvent>> events) {
        var initialState = new RPGState.LocationState(
            locationId, "unknown", Map.of(), List.of(),
            Map.of(), List.of(), List.of(), Instant.now()
        );
        
        return EventSourcing.fromEvents(initialState, events, this::applyEventToLocationState);
    }
    
    private RPGState.PlayerState applyEventToPlayerState(RPGState.PlayerState state, RPGEvent event) {
        return switch (event) {
            case RPGEvent.PlayerCreated created -> new RPGState.PlayerState(
                created.playerId(), created.name(), "village", 100, Map.of(), Map.of(),
                List.of(), List.of(), List.of(), created.occurredAt()
            );
            
            case RPGEvent.PlayerMovedToLocation moved -> new RPGState.PlayerState(
                state.playerId(), state.name(), moved.toLocationId(), state.health(),
                state.skills(), state.relationships(), state.completedQuests(),
                state.activeQuests(), state.actionHistory(), moved.occurredAt()
            );
            
            case RPGEvent.PlayerHealthChanged healthChanged -> new RPGState.PlayerState(
                state.playerId(), state.name(), state.currentLocationId(), healthChanged.newHealth(),
                state.skills(), state.relationships(), state.completedQuests(),
                state.activeQuests(), state.actionHistory(), healthChanged.occurredAt()
            );
            
            case RPGEvent.QuestStarted questStarted -> {
                var newActiveQuests = new ArrayList<>(state.activeQuests());
                newActiveQuests.add(questStarted.questId());
                yield new RPGState.PlayerState(
                    state.playerId(), state.name(), state.currentLocationId(), state.health(),
                    state.skills(), state.relationships(), state.completedQuests(),
                    newActiveQuests, state.actionHistory(), questStarted.occurredAt()
                );
            }
            
            case RPGEvent.ActionPerformed actionPerformed -> {
                var newActionHistory = new ArrayList<>(state.actionHistory());
                newActionHistory.add(new RPGState.ActionHistory(
                    actionPerformed.actionType(),
                    actionPerformed.target(),
                    actionPerformed.outcome(),
                    state.currentLocationId(),
                    actionPerformed.occurredAt()
                ));
                yield new RPGState.PlayerState(
                    state.playerId(), state.name(), state.currentLocationId(), state.health(),
                    state.skills(), state.relationships(), state.completedQuests(),
                    state.activeQuests(), newActionHistory, actionPerformed.occurredAt()
                );
            }
            
            default -> state; // Event doesn't affect player state
        };
    }
    
    private RPGState.LocationState applyEventToLocationState(RPGState.LocationState state, RPGEvent event) {
        return switch (event) {
            case RPGEvent.PlayerMovedToLocation moved -> {
                if (moved.toLocationId().equals(state.locationId())) {
                    var newOccupants = new ArrayList<>(state.currentOccupants());
                    if (!newOccupants.contains(moved.playerId())) {
                        newOccupants.add(moved.playerId());
                    }
                    yield new RPGState.LocationState(
                        state.locationId(), state.type(), state.properties(), state.visitedBy(),
                        state.timeSpentBy(), state.itemsFound(), newOccupants, moved.occurredAt()
                    );
                } else if (moved.fromLocationId().equals(state.locationId())) {
                    var newOccupants = new ArrayList<>(state.currentOccupants());
                    newOccupants.remove(moved.playerId());
                    yield new RPGState.LocationState(
                        state.locationId(), state.type(), state.properties(), state.visitedBy(),
                        state.timeSpentBy(), state.itemsFound(), newOccupants, moved.occurredAt()
                    );
                } else {
                    yield state;
                }
            }
            
            case RPGEvent.ItemDiscovered itemDiscovered -> {
                if (itemDiscovered.locationId().equals(state.locationId())) {
                    var newItemsFound = new ArrayList<>(state.itemsFound());
                    if (!newItemsFound.contains(itemDiscovered.itemId())) {
                        newItemsFound.add(itemDiscovered.itemId());
                    }
                    yield new RPGState.LocationState(
                        state.locationId(), state.type(), state.properties(), state.visitedBy(),
                        state.timeSpentBy(), newItemsFound, state.currentOccupants(), itemDiscovered.occurredAt()
                    );
                } else {
                    yield state;
                }
            }
            
            default -> state; // Event doesn't affect location state
        };
    }
    
    // === Caching ===
    
    private RPGState.PlayerState getCachedPlayerState(String playerId) {
        var cacheTime = cacheTimestamps.get("player:" + playerId);
        if (cacheTime != null && Instant.now().minusMillis(CACHE_DURATION_MS).isBefore(cacheTime)) {
            return playerStateCache.get(playerId);
        }
        return null;
    }
    
    private RPGState.LocationState getCachedLocationState(String locationId) {
        var cacheTime = cacheTimestamps.get("location:" + locationId);
        if (cacheTime != null && Instant.now().minusMillis(CACHE_DURATION_MS).isBefore(cacheTime)) {
            return locationStateCache.get(locationId);
        }
        return null;
    }
    
    private void cachePlayerState(String playerId, RPGState.PlayerState state) {
        playerStateCache.put(playerId, state);
        cacheTimestamps.put("player:" + playerId, Instant.now());
    }
    
    private void cacheLocationState(String locationId, RPGState.LocationState state) {
        locationStateCache.put(locationId, state);
        cacheTimestamps.put("location:" + locationId, Instant.now());
    }
    
    private void invalidatePlayerCache(String playerId) {
        playerStateCache.remove(playerId);
        cacheTimestamps.remove("player:" + playerId);
    }
    
    private void invalidateLocationCache(String locationId) {
        locationStateCache.remove(locationId);
        cacheTimestamps.remove("location:" + locationId);
    }
}
