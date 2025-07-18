# Enhanced Context Management System

This document describes the new generic context management system with D&D plugin support that solves the "torch teleportation" problem and maintains world state consistency.

## Problem Solved

**Before:** NPCs forget conversations, items teleport between locations, quest progress is lost, and the world state becomes inconsistent.

**After:** Complete world state tracking with validation ensures that:
- The torch stays in the snake chamber where you found it
- Baldwick remembers your childhood apple-snitching
- Bargle knows you're hunting him
- Equipment is destroyed by rust monsters as per D&D rules

## Architecture Overview

```
┌─────────────────────────────────────────┐
│           Claude API                    │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│    GenericGameContextManager            │
│  - Manages world state                  │
│  - Validates actions                    │
│  - Handles AI interactions             │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│         Plugin System                   │
│  ┌─────────────────────────────────────┐ │
│  │     TSRBasicDnDPlugin               │ │
│  │  - D&D specific rules               │ │
│  │  - Equipment vulnerability          │ │
│  │  - Combat validation                │ │
│  │  - Spell system                     │ │
│  └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│         JSON State Files                │
│  - tsr_basic_adventure.json (static)   │
│  - adventure_state.json (dynamic)      │
└─────────────────────────────────────────┘
```

## Key Components

### 1. GenericGameContextManager
- **Location**: `src/main/java/com/eventsourcing/gameSystem/context/GenericGameContextManager.java`
- **Purpose**: Core context management with plugin support
- **Features**: State validation, AI integration, event timeline

### 2. TSRBasicDnDPlugin
- **Location**: `src/main/java/com/eventsourcing/gameSystem/plugins/dnd/TSRBasicDnDPlugin.java`
- **Purpose**: D&D-specific rule enforcement
- **Features**: Equipment destruction, class restrictions, spell validation

### 3. Enhanced Adventure State
- **Location**: `adventure_state.json`
- **Purpose**: Dynamic world state tracking
- **Contains**: Character stats, NPC memories, quest progress, timeline

### 4. Integration Layer
- **Location**: `src/main/java/com/eventsourcing/gameSystem/DnDBasicGameSystem.java`
- **Purpose**: Bridge between existing system and new context manager
- **Features**: Backward compatibility, enhanced validation

## Usage Examples

### Basic Setup

```java
// Initialize AI service
AIConfig aiConfig = new AIConfig();
ClaudeAIService aiService = new ClaudeAIService(aiConfig);

// Create enhanced game system
DnDBasicGameSystem gameSystem = new DnDBasicGameSystem();
gameSystem.initializeContextManager(aiService);

// Get context manager
GenericGameContextManager contextManager = gameSystem.getContextManager();
```

### Processing User Input

```java
// User says: "I attack the rust monster with my sword"
String response = gameSystem.processUserInput(userInput);

// The system will:
// 1. Validate the action against D&D rules
// 2. Warn about metal equipment destruction
// 3. Update world state accordingly
// 4. Return AI response with validation results
```

### Manual State Updates

```java
// Update character location
ObjectMapper mapper = new ObjectMapper();
var changes = mapper.createObjectNode();
changes.put("player_location", "cave_entrance");
changes.put("event", "player_movement");

contextManager.updateState(changes);
```

### Adding Custom Validation

```java
// Create a new plugin
public class CustomGamePlugin implements GameSystemPlugin {
    @Override
    public ValidationResult validateAction(JsonNode action, JsonNode context) {
        // Your custom validation logic
        return ValidationResult.valid(getName());
    }
    
    // Implement other required methods...
}

// Register the plugin
contextManager.registerPlugin(new CustomGamePlugin());
```

## File Structure

```
src/main/java/com/eventsourcing/gameSystem/
├── context/
│   ├── GenericGameContextManager.java     # Core context manager
│   └── GameContextManagerFactory.java     # Factory for setup
├── plugins/
│   ├── GameSystemPlugin.java              # Plugin interface
│   ├── ValidationResult.java              # Validation result class
│   └── dnd/
│       └── TSRBasicDnDPlugin.java         # D&D rules plugin
├── examples/
│   └── EnhancedDnDExample.java            # Usage examples
└── DnDBasicGameSystem.java                # Enhanced game system

src/main/resources/dnd/
├── tsr_basic_adventure.json               # Static adventure data
└── basic.txt                              # Original adventure text

adventure_state.json                       # Dynamic world state
```

## Configuration

### Environment Variables
```bash
# Required for Claude API
ANTHROPIC_API_KEY=your_claude_api_key_here

# Optional configuration
CONTEXT_VALIDATION_STRICT=true
CONTEXT_AUTO_SAVE=true
```

### AI Configuration
```java
AIConfig aiConfig = new AIConfig();
// Uses environment variables or defaults
```

## Cost Analysis

| Component | Cost | Alternative |
|-----------|------|-------------|
| **Storage** | $0/month (local JSON) | Vector DB: $200-500/month |
| **AI API** | $10-50/month (Claude) | GPT-4: $15-60/month |
| **Infrastructure** | $0/month | Enterprise: $300+/month |
| **Total** | **$10-50/month** | **$500-1000+/month** |

## Benefits

### Immediate
- ✅ No more item teleportation
- ✅ NPCs remember interactions  
- ✅ Quest progress persistence
- ✅ Equipment vulnerability tracking
- ✅ Rule validation

### Long-term
- 🔄 Easy to add new game systems
- 🔄 Plugin-based extensibility
- 🔄 Cost-effective scaling
- 🔄 No vendor lock-in
- 🔄 Local data control

## D&D Rules Enforced

### Equipment System
- **Rust Monsters**: Destroy metal items on contact
- **Class Restrictions**: Clerics can't use edged weapons
- **Encumbrance**: STR-based carrying capacity
- **Armor Class**: Automatic AC calculation

### Combat System
- **Hit Rolls**: d20 + modifiers vs. AC
- **Saving Throws**: Class-based save tables
- **Initiative**: Dexterity-based turn order
- **Death**: 0 HP = death (potion can save)

### Magic System
- **Vancian Casting**: Spells must be memorized
- **Components**: Material component tracking
- **Class Limits**: Magic-users vs. clerics
- **Turn Undead**: Cleric-only ability

### Character System
- **Ability Scores**: 3-18 range with modifiers
- **Experience**: Treasure + monster XP
- **Level Advancement**: Doubling XP requirements
- **Alignment**: Law/Neutral/Chaos behavior

## Troubleshooting

### Common Issues

**Context Manager Not Initialized**
```java
// Error: Context manager not initialized
// Solution: Call initializeContextManager() first
gameSystem.initializeContextManager(aiService);
```

**Validation Failures**
```java
// Check validation results
List<ValidationResult> results = contextManager.validateAction(action);
for (ValidationResult result : results) {
    if (!result.isValid()) {
        System.out.println("Violation: " + result.getViolations());
        System.out.println("Suggestions: " + result.getSuggestions());
    }
}
```

**State File Issues**
```bash
# Check file permissions
ls -la adventure_state.json

# Validate JSON format
cat adventure_state.json | python -m json.tool
```

### Performance Optimization

**Large State Files**
- Implement state compression
- Archive old timeline events
- Use incremental saves

**API Rate Limits**
- Implement request queuing
- Add response caching
- Use exponential backoff

## Future Enhancements

### Planned Features
- [ ] Multi-user session support
- [ ] Real-time collaboration
- [ ] Advanced AI reasoning
- [ ] Custom rule scripting
- [ ] Visual state inspector

### Plugin Ecosystem
- [ ] Pathfinder plugin
- [ ] 5E D&D plugin
- [ ] Custom system templates
- [ ] Rule import/export
- [ ] Community plugin registry

## Contributing

To add a new game system plugin:

1. Implement `GameSystemPlugin` interface
2. Add system-specific validation rules
3. Create context enhancement logic
4. Register with context manager
5. Test with example scenarios

See `TSRBasicDnDPlugin.java` as a reference implementation.

## Support

For issues or questions:
1. Check the troubleshooting section
2. Review plugin documentation
3. Examine example code in `examples/`
4. Test with minimal reproduction case

## License

This enhancement maintains compatibility with your existing project license.
