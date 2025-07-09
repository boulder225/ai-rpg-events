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
        String lowerCommand = command.toLowerCase().trim();
        // Try to match command to a location
        for (LocationData location : currentAdventure.locations().values()) {
            if (lowerCommand.contains(location.id()) || lowerCommand.contains(location.name().toLowerCase())) {
                // Move to location if connected
                LocationData currentLoc = currentAdventure.getLocation(context.currentLocation());
                if (currentLoc != null && !currentLoc.connectsTo(location.id())) {
                    return GameResponse.error("You cannot travel directly from " + currentLoc.name() + " to " + location.name());
                }
                GameContext updatedContext = context.withLocation(location.id());
                String message = String.format(
                    "🚶 You travel to %s\n\n%s",
                    location.name(),
                    location.description()
                );
                return GameResponse.success(message, updatedContext);
            }
        }
        // Try to match command to an NPC
        for (NPCData npc : currentAdventure.npcs().values()) {
            if (lowerCommand.contains(npc.id()) || lowerCommand.contains(npc.name().toLowerCase())) {
                String message = String.format(
                    "💬 You approach %s\n\n%s greets you. Personality: %s",
                    npc.name(),
                    npc.name(),
                    npc.personality()
                );
                return GameResponse.success(message, context);
            }
        }
        // Try to match command to an encounter
        for (EncounterData encounter : currentAdventure.encounters()) {
            if (lowerCommand.contains(encounter.id()) || lowerCommand.contains(encounter.name().toLowerCase())) {
                String message = String.format(
                    "⚔️ You encounter %s! %s",
                    encounter.name(),
                    encounter.description()
                );
                return GameResponse.success(message, context);
            }
        }
        // Generic look command
        if (lowerCommand.contains("look")) {
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
        return GameResponse.error("Unknown command: " + command);
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
        List<QuickCommand> commands = new ArrayList<>();
        // Add look command
        commands.add(QuickCommand.of("👁️ Look", "/look", "Examine your surroundings"));
        // Add location travel commands
        for (LocationData location : currentAdventure.locations().values()) {
            commands.add(QuickCommand.of(
                location.icon() != null ? location.icon() : "📍",
                "/go " + location.id(),
                "Travel to " + location.name()
            ));
        }
        // Add NPC talk commands
        for (NPCData npc : currentAdventure.npcs().values()) {
            commands.add(QuickCommand.of(
                "💬",
                "/talk " + npc.id(),
                "Talk to " + npc.name()
            ));
        }
        return commands;
    }
    
    @Override
    public String getStartingLocation() {
        return "village";
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