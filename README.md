# AI-RPG Event Sourcing Platform

A minimalistic event sourcing codebase designed for AI-powered RPG platforms with autonomous agents, persistent worlds, and evolving character relationships.

## Core Concepts

This system models the persistent narrative context for AI-RPG platforms where:

- **Autonomous AI agents** (GMs, NPCs, monsters) operate with complete situational awareness
- **Persistent worlds** continue evolving when players are offline
- **Dynamic relationships** track trust, friendship, and social networks between entities
- **Complete action history** provides context for intelligent AI decision-making
- **Time-travel queries** enable point-in-time state reconstruction

## Architecture

### Domain Events Capture Living World State

```java
// Player journey events
record PlayerCreated(String eventId, String playerId, String name, Instant occurredAt) 
record PlayerMovedToLocation(String eventId, String playerId, String fromLocationId, String toLocationId, Instant occurredAt)

// Social network evolution
record RelationshipFormed(String eventId, String playerId, String npcId, String relationType, Instant occurredAt)
record ConversationOccurred(String eventId, String playerId, String npcId, String topic, String outcome, Instant occurredAt)

// NPC autonomous evolution
record NPCSkillLearned(String eventId, String npcId, String skillName, int level, String learnedFrom, Instant occurredAt)
record NPCGoalChanged(String eventId, String npcId, String oldGoal, String newGoal, String reason, Instant occurredAt)
```

### Rich State Models for AI Context

```java
// Complete player context for AI agents
record PlayerState(
    String playerId,
    String currentLocationId,
    Map<String, Relationship> relationships,
    List<ActionHistory> actionHistory,
    List<String> activeQuests
)

// NPC state with autonomous evolution tracking
record NPCState(
    String npcId,
    String currentGoal,
    Map<String, Integer> skills,
    List<String> knownPlayers
)
```

### Functional Command Processing

```java
// Execute commands with full state context
commandHandler.executePlayerCommand(playerId, playerState -> 
    RPGBusinessLogic.performAction(playerState, command)
);

// Query state at any point in time
var pastState = commandHandler.getPlayerStateAt(playerId, timestamp);
```

## Key Features

### 1. Persistent Narrative Context
Every player interaction, relationship change, and world event is captured as immutable events that build a complete narrative history.

### 2. Autonomous Agent Intelligence
AI agents have access to complete context including player history, relationships, current goals, and world state to make intelligent decisions.

### 3. Evolving Social Networks
Dynamic relationship tracking with trust levels, interaction history, and emotional context that influences future encounters.

### 4. Time-Travel Queries
Query any entity's state at any point in time, enabling "what if" scenarios and historical analysis.

### 5. Concurrency Control
Optimistic concurrency with retry logic ensures data consistency in multi-agent environments.

## Quick Start

### Run the Demo

```bash
# Build and run the demonstration
./gradlew run

# Run tests
./gradlew test
```

### Create Your First AI Agent Scenario

```java
// Initialize the system
var eventStore = new InMemoryEventStore<RPGEvent>();
var commandHandler = new RPGCommandHandler(eventStore);

// Create a player
commandHandler.executePlayerCommand("player-001", playerState -> 
    RPGBusinessLogic.createPlayer(new RPGCommand.CreatePlayer(
        UUID.randomUUID().toString(),
        "player-001",
        "Aria the Explorer",
        Instant.now()
    ))
);

// Create an autonomous NPC
commandHandler.executeNPCCommand("npc-001", npcState -> 
    RPGBusinessLogic.createNPC(new RPGCommand.CreateNPC(
        UUID.randomUUID().toString(),
        "npc-001",
        "Gideon the Trader",
        "merchant",
        "town-square",
        Instant.now()
    ))
);

// Player discovers a location
commandHandler.executeLocationCommand("forest-clearing", locationState -> 
    RPGBusinessLogic.discoverLocation(new RPGCommand.DiscoverLocation(
        UUID.randomUUID().toString(),
        "player-001",
        "forest-clearing",
        "wilderness",
        Instant.now()
    ))
);

// Form relationships through interaction
commandHandler.executePlayerCommand("player-001", playerState -> 
    RPGBusinessLogic.initiateConversation(playerState, new RPGCommand.InitiateConversation(
        UUID.randomUUID().toString(),
        "player-001",
        "npc-001",
        "trade_opportunities",
        Instant.now()
    ))
);
```

## Use Cases for AI-RPG Platforms

### 1. Autonomous Game Masters
AI GMs can access complete player context to create personalized narratives:

```java
var playerState = commandHandler.getPlayerState(playerId);
// AI has access to:
// - Complete action history
// - All relationships and trust levels  
// - Current quests and goals
// - Location visit patterns
```

### 2. Evolving NPCs
NPCs can develop autonomously based on world events:

```java
// NPC learns new skills
commandHandler.executeNPCCommand(npcId, npcState -> 
    RPGBusinessLogic.npcLearnSkill(npcState, command)
);

// AI sets new goals based on world state
commandHandler.executeNPCCommand(npcId, npcState -> 
    RPGBusinessLogic.setNPCGoal(npcState, "expand_trade_routes", "market_demand_increased")
);
```

### 3. Dynamic World Events
World continues evolving with player actions having lasting consequences:

```java
// Trigger autonomous world events
RPGBusinessLogic.triggerWorldEvent("dragon_awakening", "Ancient dragon stirs", affectedEntities);
```

### 4. Intelligent Relationship Systems
Track complex social dynamics that influence gameplay:

```java
// Relationships evolve based on interactions
var relationship = playerState.relationships().get(npcId);
if (relationship.trustLevel() > 75) {
    // Unlock special dialogue options
    // NPC shares secrets
    // Access to exclusive quests
}
```

## Implementation Patterns

### Event Sourcing Core
- **Generic EventStore interface** works with any domain
- **Functional command handlers** with immutable state
- **Optimistic concurrency** with automatic retry logic
- **Point-in-time queries** for historical analysis

### Domain Modeling
- **Sealed interfaces** for type-safe event hierarchies
- **Record types** for immutable data structures
- **Stream-based organization** (player, NPC, location streams)
- **Rich domain models** capturing complete context

### AI Integration Points
- **Complete state reconstruction** from event streams
- **Historical context queries** for informed decision-making
- **Relationship-aware processing** for social interactions
- **Goal-driven NPC behavior** with autonomous evolution

## Technical Requirements

- **Java 21** with preview features (sealed classes, records, pattern matching)
- **Gradle 8.5+** with Kotlin DSL
- **In-memory event store** (easily replaceable with persistent storage)
- **JUnit 5** for comprehensive testing

## Extension Points

### Custom Event Types
Add new event types by extending the `RPGEvent` sealed interface:

```java
record PlayerEquipmentChanged(String eventId, String playerId, String itemId, String slot, Instant occurredAt) implements RPGEvent {}
```

### Business Logic Extensions
Extend `RPGBusinessLogic` with new command processors:

```java
public static CommandResult<RPGEvent> processCustomCommand(StateType currentState, CustomCommand command) {
    // Validation logic
    // Generate events
    return new CommandResult.Success<>(events);
}
```

### State Model Extensions
Add new fields to state records as needed:

```java
record PlayerState(
    // ... existing fields
    Map<String, String> equipment,
    List<String> inventoryItems
) {}
```

## Event Store Implementation

The system uses a generic event store interface that can be implemented with any persistence technology:

```java
// Current: In-memory for development
var eventStore = new InMemoryEventStore<RPGEvent>();

// Future: Replace with persistent storage
// var eventStore = new PostgresEventStore<RPGEvent>(connectionString);
// var eventStore = new MongoEventStore<RPGEvent>(mongoClient);
```

## Testing Strategy

Comprehensive test coverage for:
- **Event sourcing mechanics** (append, read, concurrency)
- **State reconstruction** from event streams
- **Business logic validation** with various scenarios
- **Time-based queries** and historical state access
- **Concurrent modification handling** with retry logic

## Performance Considerations

- **Stream-based partitioning** enables horizontal scaling
- **In-memory caching** of frequently accessed states
- **Snapshot support** for large event streams (future enhancement)
- **Async processing** capabilities for AI agent operations

## Next Steps

1. **Add persistence layer** (PostgreSQL, MongoDB, or EventStore)
2. **Implement snapshots** for performance optimization
3. **Add projection system** for read models and analytics
4. **Create AI agent integration** with external ML services
5. **Build Web3 integration** for stablecoin payments and NFT assets

---

This minimalistic codebase provides the foundation for building sophisticated AI-RPG platforms with autonomous agents, persistent worlds, and evolving narratives powered by comprehensive event sourcing.
