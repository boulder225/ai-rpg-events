package com.eventsourcing.gameSystem.plugins.dnd;

import com.eventsourcing.gameSystem.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import com.eventsourcing.gameSystem.core.NPCData;
import com.eventsourcing.gameSystem.core.EncounterData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * D&D Adventure loader that converts TSR Basic adventure data
 * to the generic AdventureData format.
 */
public class DnDAdventureLoader {
    
    private static final Logger log = LoggerFactory.getLogger(DnDAdventureLoader.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Original D&D adventure format from existing JSON
    private record OriginalAdventure(
        String title,
        String description,
        String settingInfo,
        List<String> hooks,
        List<OriginalLocation> locations,
        List<OriginalNPC> npcs,
        List<OriginalEncounter> encounters,
        List<String> treasures,
        Map<String, String> lore
    ) {}
    
    private record OriginalLocation(
        String id,
        String name,
        String description,
        String type,
        List<String> features,
        List<String> connections,
        Map<String, String> secrets
    ) {}
    
    private record OriginalNPC(
        String id,
        String name,
        String race,
        String occupation,
        String personality,
        String motivation,
        String appearance,
        List<String> knowledge,
        Map<String, String> relationships
    ) {}
    
    private record OriginalEncounter(
        String id,
        String name,
        String type,
        String description,
        String trigger,
        int challengeRating,
        List<String> creatures,
        Map<String, String> tactics
    ) {}
    
    /**
     * Load TSR Basic D&D adventure and convert to generic format.
     */
    public AdventureData loadTSRBasicAdventure() {
        try {
            InputStream inputStream = getClass().getResourceAsStream("/dnd/tsr_basic_adventure.json");
            if (inputStream == null) {
                return createFallbackAdventure();
            }
            
            OriginalAdventure original = objectMapper.readValue(inputStream, OriginalAdventure.class);
            return convertToGenericFormat(original);
            
        } catch (IOException e) {
            log.error("Failed to load TSR Basic adventure: {}", e.getMessage());
            return createFallbackAdventure();
        }
    }
    
    /**
     * Convert original D&D adventure format to generic AdventureData.
     */
    private AdventureData convertToGenericFormat(OriginalAdventure original) {
        // Convert locations to generic format with icons
        Map<String, LocationData> locations = original.locations().stream()
            .collect(Collectors.toMap(
                OriginalLocation::id,
                this::convertLocation
            ));
        
        // Convert NPCs to generic format
        Map<String, NPCData> npcs = original.npcs().stream()
            .collect(Collectors.toMap(
                OriginalNPC::id,
                this::convertNPC
            ));
        
        // Convert encounters to generic format
        List<EncounterData> encounters = original.encounters().stream()
            .map(this::convertEncounter)
            .collect(Collectors.toList());
        
        return new AdventureData(
            original.title(),
            original.description(),
            original.settingInfo(),
            original.hooks(),
            locations,
            npcs,
            encounters,
            original.treasures(),
            original.lore()
        );
    }
    
    /**
     * Convert original location to generic LocationData with appropriate icon.
     */
    private LocationData convertLocation(OriginalLocation original) {
        String icon = getLocationIcon(original.id(), original.type());
        
        return new LocationData(
            original.id(),
            original.name(),
            original.description(),
            original.type(),
            original.features(),
            original.connections(),
            original.secrets(),
            icon
        );
    }
    
    /**
     * Get appropriate emoji icon for D&D locations.
     */
    private String getLocationIcon(String locationId, String type) {
        return switch (locationId) {
            case "village" -> "🏘️";
            case "cave_entrance" -> "🕳️";
            case "first_corridor" -> "🌑";
            case "snake_chamber" -> "🐍";
            case "aleena_chamber" -> "⛪";
            case "ghoul_corridor" -> "💀";
            case "locked_door_area" -> "🚪";
            case "bargle_chamber" -> "🧙‍♂️";
            case "exit_passage" -> "🌅";
            default -> switch (type) {
                case "town", "village" -> "🏘️";
                case "wilderness" -> "🌲";
                case "dungeon" -> "⚫";
                default -> "📍";
            };
        };
    }
    
    /**
     * Convert original NPC to generic NPCData.
     */
    private NPCData convertNPC(OriginalNPC original) {
        return new NPCData(
            original.id(),
            original.name(),
            original.race(),
            original.occupation(),
            original.personality(),
            original.motivation(),
            original.appearance(),
            original.knowledge(),
            original.relationships()
        );
    }
    
    /**
     * Convert original encounter to generic EncounterData.
     */
    private EncounterData convertEncounter(OriginalEncounter original) {
        return new EncounterData(
            original.id(),
            original.name(),
            original.type(),
            original.description(),
            original.trigger(),
            original.challengeRating(),
            original.creatures(),
            original.tactics()
        );
    }
    
    /**
     * Create fallback adventure if JSON loading fails.
     */
    private AdventureData createFallbackAdventure() {
        Map<String, LocationData> locations = Map.of(
            "village", new LocationData(
                "village", "Your Home Village",
                "A small frontier village with dirt roads. Home to simple folk including Armorer Baldwick.",
                "town", 
                List.of("Baldwick's Armor Shop", "Village Inn", "Simple houses"),
                List.of("cave_entrance"),
                Map.of("childhood_memories", "You used to snitch apples from Baldwick's yard"),
                "🏘️"
            ),
            "cave_entrance", new LocationData(
                "cave_entrance", "Cave Entrance", 
                "The entrance to a dark cave system in the nearby hills. Peaceful outside, but danger lurks within.",
                "wilderness",
                List.of("Rocky entrance", "Natural light", "Peaceful surroundings"),
                List.of("village", "first_corridor"),
                Map.of("ambush_site", "Goblins often scout this area"),
                "🕳️"
            ),
            "first_corridor", new LocationData(
                "first_corridor", "Dark Corridor",
                "A dark, musty passage leading deeper into the hill. Single route inward.",
                "dungeon",
                List.of("Dark and musty", "Single passage", "Echo of footsteps"),
                List.of("cave_entrance", "snake_chamber"),
                Map.of(),
                "🌑"
            ),
            "snake_chamber", new LocationData(
                "snake_chamber", "Snake's Treasure Chamber",
                "A wider chamber containing a huge rattlesnake nearly ten feet long. Gold and silver coins scattered on the floor.",
                "dungeon",
                List.of("Treasure scattered on floor", "Dark corners", "Musty air"),
                List.of("first_corridor", "aleena_chamber"),
                Map.of("treasure_origin", "Coins belonged to someone who failed to kill the snake", "hidden_gem", "A pearl is hidden worth 100 gp"),
                "🐍"
            )
        );
        
        Map<String, NPCData> npcs = Map.of(
            "baldwick", new NPCData(
                "baldwick", "Armorer Baldwick", "Human", "Armorer and Shopkeeper",
                "Jolly, graying, friendly but business-minded",
                "Make a living selling quality armor and weapons",
                "Gray-haired, robust armorer with friendly demeanor",
                List.of("Armor types and prices", "Local gossip", "Character's childhood"),
                Map.of("player", "childhood_acquaintance")
            ),
            "bargle", new NPCData(
                "bargle", "Bargle the Bandit", "Human", "Evil Magic-User and Bandit",
                "Chaotic, selfish, cunning, and dangerous",
                "Gain power and treasure through theft and murder",
                "Tall bearded human in black robes",
                List.of("Arcane magic", "Invisibility spells", "Charm spells", "Local area"),
                Map.of("player", "primary_enemy", "goblin_servant", "master")
            )
        );
        
        return new AdventureData(
            "The Caves of Chaos - TSR Basic D&D Solo Adventure",
            "A solo adventure for beginning characters, featuring the search for Bargle the bandit in mysterious caves.",
            "Your home town is just a small place with dirt roads. The bandit Bargle has been terrorizing your town.",
            List.of(
                "Find Bargle the bandit who has been terrorizing your village",
                "Explore the dangerous caves in the nearby hills",
                "Discover treasure guarded by monsters",
                "Become a hero by bringing peace to your village"
            ),
            locations,
            npcs,
            List.of(), // encounters
            List.of(
                "Hundreds of gold and silver coins (50 gp, 100 sp, 20 ep, 7 pp)",
                "Hidden pearl worth 100 gp",
                "Potion of Healing from church reward",
                "Potion of Growth in Bargle's black velvet bag"
            ),
            Map.of(
                "Bargle", "A chaotic magic-user who has been terrorizing the village",
                "Combat_System", "Roll d20, need 12+ to hit goblin, 11+ to hit snake. Player has 8 hp, AC 4",
                "Character_Stats", "Strength 17 (+2 bonus), Fighter class with chain mail"
            )
        );
    }
}