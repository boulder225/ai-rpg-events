package com.eventsourcing.gameSystem.plugins.dnd;

import com.eventsourcing.gameSystem.core.*;
import java.util.*;
import com.eventsourcing.gameSystem.core.NPCData;

/**
 * D&D Basic Rules implementation of the GameSystem interface.
 * Provides classic D&D fantasy RPG experience with TSR Basic adventure content.
 */
public class DnDGameSystem implements GameSystem {
    
    private final DnDAdventureLoader adventureLoader;
    private AdventureData currentAdventure;
    
    public DnDGameSystem() {
        this.adventureLoader = new DnDAdventureLoader();
        // Load the default TSR Basic adventure
        this.currentAdventure = loadAdventure("tsr_basic");
    }
    
    @Override
    public String getSystemName() {
        return "D&D Basic";
    }
    
    @Override
    public String getSystemDescription() {
        return "Classic TSR Basic D&D Rules - Fantasy adventure with dungeons, dragons, and heroic quests";
    }
    
    @Override
    public AdventureData loadAdventure(String adventureId) {
        return adventureLoader.loadTSRBasicAdventure();
    }
    
    @Override
    public GameContext createInitialContext(String playerName) {
        // D&D Basic starting stats
        Map<String, Integer> stats = Map.of(
            "strength", 17,
            "dexterity", 11, 
            "intelligence", 9,
            "wisdom", 8,
            "constitution", 16,
            "charisma", 14
        );
        
        Map<String, String> characterData = Map.of(
            "class", "Fighter",
            "race", "Human",
            "equipment", "Chain Mail (AC 4), Sword, Dagger, Lantern",
            "background", "Village hero seeking the bandit Bargle"
        );
        
        Map<String, Object> gameSpecificData = Map.of(
            "armorClass", 4,
            "level", 1,
            "experience", 0
        );
        
        return new GameContext(
            "player_" + System.currentTimeMillis(),
            playerName,
            getStartingLocation(),
            8, // D&D Basic starting HP
            8, // Max HP
            stats,
            characterData,
            gameSpecificData
        );
    }
    
    @Override
    public GameResponse processAction(GameContext context, String command) {
        // Process D&D-specific commands
        String lowerCommand = command.toLowerCase().trim();
        
        return switch (lowerCommand) {
            case "/look around", "/look" -> processLookAction(context);
            case "/attack goblin" -> processAttackAction(context, "goblin");
            case "/talk tavern_keeper" -> processTalkAction(context, "tavern_keeper");
            case "/go cave", "/enter cave", "/move cave" -> 
                processMoveAction(context, "cave_entrance");
            case "/go village", "/return village", "/move village" -> 
                processMoveAction(context, "village");
            case "/go corridor", "/enter corridor", "/move corridor" -> 
                processMoveAction(context, "first_corridor");
            case "/go chamber", "/enter chamber", "/move chamber" -> 
                processMoveAction(context, "aleena_chamber");
            default -> GameResponse.error("Unknown command: " + command);
        };
    }
    
    @Override
    public Map<String, LocationData> getLocationData() {
        // Convert D&D adventure locations to generic format
        Map<String, LocationData> locations = new HashMap<>();
        
        if (currentAdventure != null) {
            currentAdventure.locations().forEach((id, location) -> {
                locations.put(id, location);
            });
        }
        
        return locations;
    }
    
    @Override
    public List<QuickCommand> getQuickCommands() {
        return List.of(
            QuickCommand.of("👁️ Look", "/look around", "Examine your surroundings"),
            QuickCommand.of("💬 Talk", "/talk tavern_keeper", "Speak with NPCs"),
            QuickCommand.of("⚔️ Fight", "/attack goblin", "Attack enemies"),
            QuickCommand.of("🕳️ Go Cave", "/go cave", "Travel to cave entrance"),
            QuickCommand.of("��️ Go Village", "/go village", "Return to village"),
            QuickCommand.of("🌑 Go Corridor", "/go corridor", "Enter dark corridor")
        );
    }
    
    @Override
    public String getStartingLocation() {
        return "village";
    }
    
    // Private action processing methods
    private GameResponse processLookAction(GameContext context) {
        String locationId = context.currentLocation();
        LocationData location = currentAdventure.getLocation(locationId);
        
        if (location == null) {
            return GameResponse.error("Unknown location: " + locationId);
        }
        
        String message = String.format(
            "🔍 You find yourself in %s\n\n%s\n\nFeatures: %s",
            location.name(),
            location.description(),
            String.join(", ", location.features())
        );
        
        return GameResponse.success(message, context);
    }
    
    private GameResponse processAttackAction(GameContext context, String target) {
        // Simple D&D combat simulation
        Random dice = new Random();
        int attackRoll = dice.nextInt(20) + 1; // d20
        int damageRoll = dice.nextInt(6) + 1;  // d6
        
        String message;
        if (attackRoll >= 12) { // Hit AC 12 (goblin)
            message = String.format(
                "⚔️ You swing your sword at the %s!\n" +
                "🎲 Attack roll: %d - HIT!\n" +
                "💥 You deal %d damage to the %s!",
                target, attackRoll, damageRoll, target
            );
        } else {
            message = String.format(
                "⚔️ You swing your sword at the %s!\n" +
                "🎲 Attack roll: %d - MISS!\n" +
                "😤 Your attack misses the %s!",
                target, attackRoll, target
            );
        }
        
        return GameResponse.success(message, context);
    }
    
    private GameResponse processTalkAction(GameContext context, String npcId) {
        NPCData npc = currentAdventure.getNPC(npcId);
        if (npc == null) {
            return GameResponse.error("NPC not found: " + npcId);
        }
        
        String message = String.format(
            "💬 You approach %s\n\n" +
            "%s greets you warmly. \"%s\"\n\n" +
            "Personality: %s",
            npc.name(),
            npc.name(),
            "Welcome, brave adventurer! I've heard tales of the bandit Bargle causing trouble.",
            npc.personality()
        );
        
        return GameResponse.success(message, context);
    }
    
    private GameResponse processMoveAction(GameContext context, String targetLocation) {
        LocationData currentLoc = currentAdventure.getLocation(context.currentLocation());
        LocationData targetLoc = currentAdventure.getLocation(targetLocation);
        
        if (targetLoc == null) {
            return GameResponse.error("Unknown location: " + targetLocation);
        }
        
        if (currentLoc != null && !currentLoc.connectsTo(targetLocation)) {
            return GameResponse.error("You cannot travel directly from " + currentLoc.name() + " to " + targetLoc.name());
        }
        
        GameContext updatedContext = context.withLocation(targetLocation);
        String message = String.format(
            "🚶 You travel to %s\n\n%s",
            targetLoc.name(),
            targetLoc.description()
        );
        
        return GameResponse.success(message, updatedContext);
    }

    @Override
    public String getAdventureContext(AdventureData adventure, String locationId) {
        LocationData location = adventure.getLocation(locationId);
        if (location == null) return "Unknown location.";
        return String.format("You are at %s: %s\nFeatures: %s", location.name(), location.description(), String.join(", ", location.features()));
    }

    @Override
    public String getRulesContext() {
        return "D&D Basic Rules: Roll a d20 for actions, AC 4 = Chain Mail, 8 HP starting, classic fantasy classes and monsters.";
    }

    @Override
    public Map<String, Object> getMetadata() {
        return Map.of(
            "systemName", getSystemName(),
            "description", getSystemDescription(),
            "quickCommands", getQuickCommands(),
            "locations", getLocationData(),
            "startingLocation", getStartingLocation()
        );
    }
}