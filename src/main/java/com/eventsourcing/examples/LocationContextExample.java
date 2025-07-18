package com.eventsourcing.examples;

import com.eventsourcing.gameSystem.context.LocationContext;
import com.eventsourcing.gameSystem.context.LocationContextManager;
import com.eventsourcing.gameSystem.core.*;
import com.eventsourcing.rpg.*;
import com.eventsourcing.core.infrastructure.InMemoryEventStore;
import java.time.Instant;
import java.util.*;

/**
 * Example demonstrating the enhanced location context awareness system.
 * Shows how the system now provides rich, contextual information when 
 * characters move between locations.
 */
public class LocationContextExample {
    
    public static void main(String[] args) {
        // KISS: Example disabled due to event sourcing removal
        System.out.println("LocationContextExample is currently disabled due to event sourcing refactor.");
    }
    
    private static void demonstrateLocationContext(LocationContextManager manager, 
                                                 String playerId, 
                                                 String locationId) {
        
        LocationContext context = manager.getFullLocationContext(locationId, playerId);
        
        System.out.println("📍 Location: " + context.name());
        System.out.println("   Type: " + context.type());
        System.out.println("   Description: " + context.description());
        
        if (!context.features().isEmpty()) {
            System.out.println("   Features: " + String.join(", ", context.features()));
        }
        
        if (!context.connections().isEmpty()) {
            System.out.println("   Exits:");
            context.connections().forEach((direction, target) -> 
                System.out.println("     " + direction + " → " + target));
        }
        
        if (context.requiresLight()) {
            System.out.println("   ⚠️  Requires light source");
        }
        
        if (context.hasSecrets()) {
            System.out.println("   🗝️  Contains secrets");
        }
        
        System.out.println("   Explored: " + (context.hasBeenExplored() ? "Yes" : "No"));
    }
    
    /**
     * Create sample adventure data for demonstration
     */
    private static AdventureData createSampleAdventure() {
        
        // Create sample locations
        var village = new LocationData(
            "village",
            "Your Home Village",
            "A small frontier village with dirt roads. Home to simple folk.",
            "town",
            List.of("Baldwick's Armor Shop", "Village Inn", "Simple houses"),
            List.of("cave_entrance"),
            Map.of("childhood_memories", "You used to snitch apples from Baldwick's yard"),
            "🏘️"
        );
        
        var caveEntrance = new LocationData(
            "cave_entrance",
            "Cave Entrance",
            "The entrance to a dark cave system in nearby hills. Peaceful outside, danger within.",
            "wilderness",
            List.of("Rocky entrance", "Natural light", "Fresh air"),
            List.of("village", "snake_chamber"),
            Map.of("ambush_site", "Goblins often scout this area"),
            "⛰️"
        );
        
        var snakeChamber = new LocationData(
            "snake_chamber",
            "Snake's Treasure Chamber",
            "A wider chamber containing a huge rattlesnake. Gold and silver coins scattered on floor.",
            "dungeon",
            List.of("Treasure scattered", "Dark corners", "Musty air"),
            List.of("cave_entrance", "aleena_chamber"),
            Map.of(
                "treasure_origin", "Coins belonged to someone who failed to kill the snake",
                "hidden_gem", "A pearl is hidden in one corner worth 100 gp"
            ),
            "🐍"
        );
        
        var aleenaChamber = new LocationData(
            "aleena_chamber",
            "Aleena's Meditation Chamber",
            "A small cave where Aleena the cleric sits by the wall, wearing chain mail.",
            "dungeon",
            List.of("Natural meditation spot", "Stone walls", "Peaceful atmosphere"),
            List.of("snake_chamber", "ghoul_corridor"),
            Map.of("safe_haven", "Clerics often use this spot for prayer and rest"),
            "🧘"
        );
        
        // Create sample NPCs
        var baldwick = new NPCData(
            "baldwick",
            "Armorer Baldwick",
            "human",
            "Armorer and Shopkeeper",
            "Jolly, graying, friendly but business-minded",
            "Make a living selling quality armor and weapons",
            "Gray-haired, robust armorer with friendly demeanor",
            List.of("Armor types and prices", "Local gossip", "Character's childhood"),
            Map.of("player", "childhood_acquaintance")
        );
        
        var aleena = new NPCData(
            "aleena",
            "Aleena the Cleric",
            "human",
            "Cleric and Adventurer",
            "Wise, helpful, brave, and devout",
            "Help others and fight evil monsters",
            "Beautiful woman in chain mail carrying a mace",
            List.of("Clerical magic", "Monster lore", "Healing arts", "Undead creatures"),
            Map.of("player", "potential_ally")
        );
        
        // Create sample locations map
        Map<String, LocationData> locations = Map.of(
            village.id(), village,
            caveEntrance.id(), caveEntrance,
            snakeChamber.id(), snakeChamber,
            aleenaChamber.id(), aleenaChamber
        );
        // Create sample NPCs map
        Map<String, NPCData> npcs = Map.of(
            baldwick.id(), baldwick,
            aleena.id(), aleena
        );
        // Create sample encounters (simplified for demo)
        var encounters = List.<EncounterData>of();
        // Create sample treasures and lore (empty for demo)
        var treasures = List.<String>of();
        var lore = Map.<String, String>of();
        return new AdventureData(
            "demo_adventure",
            "Location Context Awareness Demo",
            "A demonstration of the enhanced location context system.",
            List.of("Demonstrate location context", "Test AI integration", "Show rich environmental details"),
            locations,
            npcs,
            encounters,
            treasures,
            lore
        );
    }
}
