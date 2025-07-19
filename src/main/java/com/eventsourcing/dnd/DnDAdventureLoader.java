package com.eventsourcing.dnd;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads D&D adventures and content from JSON resources.
 * Provides structured adventure content for AI context.
 */
public class DnDAdventureLoader {
    
    private static final Logger log = LoggerFactory.getLogger(DnDAdventureLoader.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public record Adventure(
        String title,
        String description,
        String settingInfo,
        List<String> hooks,
        List<Location> locations,
        List<NPC> npcs,
        List<Encounter> encounters,
        List<String> treasures,
        Map<String, String> lore
    ) {}
    
    public record Location(
        String id,
        String name,
        String description,
        String type,
        List<String> features,
        List<String> connections,
        Map<String, String> secrets
    ) {}
    
    public record NPC(
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
    
    public record Encounter(
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
     * Load the default starter adventure from resources.
     */
    public Adventure loadStarterAdventure() {
        try {
            InputStream inputStream = getClass().getResourceAsStream("/dnd/starter_adventure.json");
            if (inputStream == null) {
                return createDefaultAdventure();
            }
            return objectMapper.readValue(inputStream, Adventure.class);
        } catch (IOException e) {
            log.error("Failed to load adventure: {}", e.getMessage());
            return createDefaultAdventure();
        }
    }
    
    /**
     * Load the TSR Basic D&D Solo Adventure.
     */
    public Adventure loadTSRBasicAdventure() {
        try {
            InputStream inputStream = getClass().getResourceAsStream("/dnd/tsr_basic_adventure.json");
            if (inputStream == null) {
                return createTSRBasicAdventure();
            }
            return objectMapper.readValue(inputStream, Adventure.class);
        } catch (IOException e) {
            log.error("Failed to load TSR Basic adventure: {}", e.getMessage());
            return createTSRBasicAdventure();
        }
    }
    
    /**
     * Generate AI context from adventure data.
     */
    public String generateAdventureContext(Adventure adventure, String currentLocation) {
        var context = new StringBuilder();
        
        context.append("D&D ADVENTURE CONTEXT\n");
        context.append("=====================\n\n");
        
        context.append("ADVENTURE: ").append(adventure.title()).append("\n");
        context.append(adventure.description()).append("\n\n");
        
        context.append("SETTING:\n").append(adventure.settingInfo()).append("\n\n");
        
        // Current location details
        adventure.locations().stream()
            .filter(loc -> loc.id().equals(currentLocation))
            .findFirst()
            .ifPresent(location -> {
                context.append("CURRENT LOCATION: ").append(location.name()).append("\n");
                context.append(location.description()).append("\n");
                
                if (!location.features().isEmpty()) {
                    context.append("Features: ").append(String.join(", ", location.features())).append("\n");
                }
                
                if (!location.connections().isEmpty()) {
                    context.append("Exits: ").append(String.join(", ", location.connections())).append("\n");
                }
                context.append("\n");
            });
        
        // Nearby NPCs
        context.append("NOTABLE NPCS:\n");
        adventure.npcs().forEach(npc -> {
            context.append("- ").append(npc.name()).append(" (").append(npc.occupation()).append("): ");
            context.append(npc.personality()).append("\n");
        });
        context.append("\n");
        
        // Adventure hooks
        if (!adventure.hooks().isEmpty()) {
            context.append("ADVENTURE HOOKS:\n");
            adventure.hooks().forEach(hook -> context.append("- ").append(hook).append("\n"));
            context.append("\n");
        }
        
        // Lore and background
        if (!adventure.lore().isEmpty()) {
            context.append("WORLD LORE:\n");
            adventure.lore().forEach((key, value) -> 
                context.append("- ").append(key).append(": ").append(value).append("\n"));
        }
        
        return context.toString();
    }
    
    private Adventure createTSRBasicAdventure() {
        return new Adventure(
            "The Caves of Chaos - TSR Basic D&D Solo Adventure",
            "A solo adventure featuring the search for Bargle the bandit in mysterious caves near a small village.",
            "Your home village with dirt roads. Bargle the bandit has been terrorizing the locals through theft and murder.",
            List.of(
                "Find Bargle the bandit who has been terrorizing your village",
                "Explore the dangerous caves in the nearby hills", 
                "Discover treasure guarded by monsters",
                "Become a hero by bringing peace to your village"
            ),
            createTSRLocations(),
            createTSRNPCs(),
            createTSREncounters(),
            List.of(
                "Gold and silver coins (50 gp, 100 sp, 20 ep, 7 pp)",
                "Hidden pearl worth 100 gp",
                "Potion of Healing (church reward)",
                "Potion of Growth (in Bargle's bag)",
                "Assorted gems and copper pieces"
            ),
            Map.of(
                "Bargle", "Evil chaotic magic-user terrorizing the village",
                "Combat", "d20 system: 12+ to hit goblin, 11+ to hit snake. Player has 8hp, AC 4",
                "Character", "Fighter with STR 17 (+2), basic equipment: chain mail, sword, dagger",
                "Adventure", "Classic TSR Basic D&D solo adventure from 1983"
            )
        );
    }
    
    private List<Location> createTSRLocations() {
        return List.of(
            new Location(
                "village", "Your Home Village",
                "A small frontier village with dirt roads. Home to Armorer Baldwick.",
                "town", List.of("Baldwick's Shop", "Village Inn"), List.of("cave_entrance"),
                Map.of("childhood", "You used to snitch apples from Baldwick's yard")
            ),
            new Location(
                "cave_entrance", "Cave Entrance", 
                "Rocky entrance to dangerous caves in the nearby hills.",
                "wilderness", List.of("Rocky entrance"), List.of("village", "first_corridor"),
                Map.of("danger", "Peaceful outside but monsters lurk within")
            ),
            new Location(
                "snake_chamber", "Snake's Treasure Chamber",
                "Chamber with huge rattlesnake guarding hundreds of coins.",
                "dungeon", List.of("Scattered treasure"), List.of("first_corridor"),
                Map.of("treasure", "Coins from failed adventurer, hidden pearl worth 100gp")
            )
        );
    }
    
    private List<NPC> createTSRNPCs() {
        return List.of(
            new NPC(
                "baldwick", "Armorer Baldwick", "Human", "Armorer",
                "Jolly, graying, friendly but business-minded",
                "Make a living selling armor", "Gray-haired robust armorer",
                List.of("Armor prices", "Local gossip"), Map.of("player", "childhood_friend")
            ),
            new NPC(
                "aleena", "Aleena the Cleric", "Human", "Cleric and Adventurer", 
                "Wise, helpful, brave, and devout", "Help others and fight evil",
                "Beautiful woman in chain mail with mace", 
                List.of("Clerical magic", "Monster lore"), Map.of("player", "ally")
            ),
            new NPC(
                "bargle", "Bargle the Bandit", "Human", "Evil Magic-User",
                "Chaotic, selfish, cunning, dangerous", "Gain power through theft and murder",
                "Tall bearded human in black robes",
                List.of("Arcane magic", "Invisibility spells"), Map.of("player", "enemy")
            )
        );
    }
    
    private List<Encounter> createTSREncounters() {
        return List.of(
            new Encounter(
                "goblin_encounter", "Goblin Ambush", "combat",
                "Gray-skinned goblin attacks immediately with crude sword",
                "Entering first corridor", 1, List.of("Goblin (1 hp)"),
                Map.of("hit_target", "12+ on d20", "behavior", "Flees when hit")
            ),
            new Encounter(
                "snake_encounter", "Giant Rattlesnake", "combat", 
                "Ten-foot rattlesnake guards treasure hoard with poisonous bite",
                "Entering snake chamber", 2, List.of("Giant Rattlesnake (3 hp)"),
                Map.of("hit_target", "11+ on d20", "poison", "Save 12+ or 2 extra damage")
            ),
            new Encounter(
                "bargle_encounter", "Bargle the Magic-User", "boss_combat",
                "Evil magic-user uses invisibility and charm spells", 
                "Final confrontation", 3, List.of("Bargle", "Goblin Servant"),
                Map.of("spells", "Invisibility, Charm Person, Magic Missile")
            )
        );
    }
    
    /**
     * Get D&D rules context for AI.
     */
    public String generateRulesContext() {
        return """
            D&D 5E RULES CONTEXT
            ===================
            
            CORE MECHANICS:
            - All actions use d20 + ability modifier + proficiency bonus
            - Difficulty Classes: Easy (10), Medium (15), Hard (20)
            - Advantage: Roll twice, take higher
            - Disadvantage: Roll twice, take lower
            
            ABILITY SCORES:
            - Strength: Physical power, Athletics
            - Dexterity: Agility, Stealth, Acrobatics  
            - Constitution: Health, endurance
            - Intelligence: Reasoning, Investigation, Arcana
            - Wisdom: Awareness, Insight, Perception
            - Charisma: Force of personality, Persuasion, Deception
            
            COMMON ACTIONS:
            - Attack: d20 + ability + proficiency vs AC
            - Skill Check: d20 + ability + proficiency vs DC
            - Saving Throw: d20 + ability + proficiency vs DC
            - Spell Attack: d20 + spellcasting ability + proficiency vs AC
            
            ROLEPLAY GUIDELINES:
            - Stay true to character class and background
            - Consider alignment and personality traits
            - NPCs have motivations and react realistically
            - Actions have consequences in the world
            """;
    }
    
    private Adventure createDefaultAdventure() {
        return new Adventure(
            "The Lost Mine of Phandelver",
            "A classic starter adventure in the Sword Coast region. The party must rescue a captured dwarf and investigate goblin raids threatening the town of Phandalin.",
            "The Sword Coast is a region of Faerûn, a continent on the world of Toril. It's a frontier land of wilderness and ancient ruins, where adventurers seek fortune and glory.",
            List.of(
                "Gundren Rockseeker has hired you to escort supplies to Phandalin",
                "Goblin raids have been increasing along the Triboar Trail",
                "Strange happenings around the old Phandelver mine"
            ),
            List.of(
                new Location(
                    "phandalin",
                    "Phandalin",
                    "A small frontier town built on the ruins of an older settlement. Rough stone foundations and broken walls from the ancient town are visible among the newer wooden buildings.",
                    "town",
                    List.of("Shrine of Luck", "Phandalin Miner's Exchange", "The Sleeping Giant inn"),
                    List.of("triboar_trail", "ruins", "manor"),
                    Map.of("history", "Built on ruins of ancient town destroyed by orcs")
                ),
                new Location(
                    "triboar_trail",
                    "Triboar Trail",
                    "A dirt road running east-west through the wilderness. Dense woods press close on either side, and the rutted track bears signs of recent traffic.",
                    "wilderness",
                    List.of("Dense forest", "Rocky outcrops", "Hidden goblin ambush site"),
                    List.of("phandalin", "goblin_hideout"),
                    Map.of("danger", "Goblin raiders active in this area")
                )
            ),
            List.of(
                new NPC(
                    "sildar",
                    "Sildar Hallwinter",
                    "Human",
                    "Lords' Alliance Agent",
                    "Friendly but professional, devoted to law and order",
                    "Establish law and order in Phandalin",
                    "Middle-aged human male in traveling clothes and chainmail",
                    List.of("Lords' Alliance operations", "Political situation", "Goblin activity"),
                    Map.of("gundren", "ally", "klarg", "enemy")
                ),
                new NPC(
                    "toblen",
                    "Toblen Stonehill",
                    "Halfling",
                    "Innkeeper of Stonehill Inn",
                    "Friendly and talkative, eager to please guests",
                    "Run a successful inn and keep customers happy",
                    "Short halfling with a round belly and welcoming smile",
                    List.of("Local gossip", "Recent events", "Travelers' stories"),
                    Map.of("townspeople", "friendly")
                )
            ),
            List.of(
                new Encounter(
                    "goblin_ambush",
                    "Goblin Ambush",
                    "combat",
                    "Four goblins attack from roadside brush, using guerrilla tactics",
                    "Traveling along Triboar Trail",
                    1,
                    List.of("Goblin x4"),
                    Map.of("tactics", "Use cover and ranged attacks", "goal", "Capture prisoners")
                )
            ),
            List.of(
                "Potion of Healing",
                "50 gp in mixed coins", 
                "Longsword +1",
                "Spell scroll (Magic Missile)"
            ),
            Map.of(
                "Wave Echo Cave", "Ancient magical mine lost for centuries",
                "Spell Forge", "Magical forge that can enhance weapons and armor",
                "Phandelver's Pact", "Ancient alliance between humans, dwarves, and gnomes"
            )
        );
    }
}
