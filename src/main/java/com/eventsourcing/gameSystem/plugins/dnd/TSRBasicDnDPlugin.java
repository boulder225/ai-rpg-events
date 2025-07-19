package com.eventsourcing.gameSystem.plugins.dnd;

import com.eventsourcing.gameSystem.plugins.GameSystemPlugin;
import com.eventsourcing.gameSystem.plugins.ValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * TSR Basic D&D Rules Plugin
 * Implements classic D&D mechanics for the TSR Basic adventure system
 */
public class TSRBasicDnDPlugin implements GameSystemPlugin {
    
    private static final Logger LOGGER = Logger.getLogger(TSRBasicDnDPlugin.class.getName());
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // D&D Constants
    private static final List<String> METAL_ITEMS = Arrays.asList(
        "sword", "shield", "armor", "dagger", "chain_mail", "plate_mail", 
        "helmet", "iron_bands", "metal_coins"
    );
    
    private static final List<String> FIGHTER_WEAPONS = Arrays.asList(
        "sword", "dagger", "mace", "spear", "bow", "crossbow"
    );
    
    @Override
    public String getName() {
        return "TSR Basic D&D Rules Engine";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public boolean supportsGameSystem(String gameSystemType) {
        return "tsr_basic_dnd".equalsIgnoreCase(gameSystemType) || 
               "dnd".equalsIgnoreCase(gameSystemType) ||
               "basic_dnd".equalsIgnoreCase(gameSystemType);
    }
    
    @Override
    public JsonNode enhanceContext(JsonNode baseContext) {
        ObjectNode enhanced = baseContext.deepCopy();
        
        // Add D&D specific systems
        enhanced.put("combat_system", "d20_plus_modifiers");
        enhanced.put("magic_system", "vancian_casting");
        enhanced.put("alignment_system", "law_neutral_chaos");
        enhanced.put("death_at_zero_hp", true);
        
        // Add ability score modifiers if character exists
        if (baseContext.has("character_state")) {
            JsonNode character = baseContext.get("character_state");
            enhanced.set("ability_modifiers", calculateAbilityModifiers(character));
            enhanced.set("saving_throws", getSavingThrowTable(character));
            enhanced.set("character_capabilities", getCharacterCapabilities(character));
        }
        
        // Add monster information for current location
        enhanced.set("known_monsters", getKnownMonsters(baseContext));
        
        // Add equipment vulnerability warnings
        enhanced.set("equipment_warnings", getEquipmentWarnings(baseContext));
        
        return enhanced;
    }
    
    @Override
    public ValidationResult validateAction(JsonNode action, JsonNode currentContext) {
        List<String> violations = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        
        try {
            // Validate equipment rules
            violations.addAll(validateEquipmentRules(action, currentContext));
            
            // Validate combat rules
            violations.addAll(validateCombatRules(action, currentContext));
            
            // Validate magic rules
            violations.addAll(validateMagicRules(action, currentContext));
            
            // Validate alignment behavior
            violations.addAll(validateAlignmentRules(action, currentContext));
            
            // Validate character capabilities
            violations.addAll(validateCharacterCapabilities(action, currentContext));
            
            // Generate suggestions if there are violations
            if (!violations.isEmpty()) {
                suggestions.addAll(generateDnDSuggestions(action, violations, currentContext));
            }
            
        } catch (Exception e) {
            LOGGER.warning("D&D validation error: " + e.getMessage());
            violations.add("Internal validation error: " + e.getMessage());
        }
        
        return new ValidationResult(violations.isEmpty(), violations, suggestions, getName());
    }
    
    @Override
    public void beforeStateUpdate(JsonNode changes, JsonNode currentContext) {
        // Handle special D&D events before state update
        
        if (changes.has("rust_monster_attack")) {
            handleRustMonsterAttack(changes, currentContext);
        }
        
        if (changes.has("character_death")) {
            handleCharacterDeath(changes, currentContext);
        }
        
        if (changes.has("spell_casting")) {
            handleSpellCasting(changes, currentContext);
        }
        
        if (changes.has("combat_damage")) {
            handleCombatDamage(changes, currentContext);
        }
        
        LOGGER.info("D&D Plugin: Processing state changes before update");
    }
    
    @Override
    public void afterStateUpdate(JsonNode changes, JsonNode newContext) {
        // Handle D&D consequences after state update
        
        if (changes.has("treasure_found")) {
            calculateExperiencePoints(changes, newContext);
        }
        
        if (changes.has("monster_defeated")) {
            awardCombatExperience(changes, newContext);
        }
        
        if (changes.has("quest_completed")) {
            checkLevelAdvancement(changes, newContext);
        }
        
        if (changes.has("equipment_destroyed")) {
            updateArmorClass(changes, newContext);
        }
        
        LOGGER.info("D&D Plugin: Processing consequences after state update");
    }
    
    @Override
    public String enhancePrompt(String basePrompt, JsonNode context) {
        StringBuilder enhanced = new StringBuilder(basePrompt);
        
        enhanced.append("\n\n=== TSR BASIC D&D RULES ===\n");
        enhanced.append("Combat System:\n");
        enhanced.append("- Hit rolls: d20 + ability modifiers vs. target AC\n");
        enhanced.append("- Damage: weapon die + strength modifier\n");
        enhanced.append("- Initiative: simultaneous unless specified\n");
        enhanced.append("- Death at 0 HP (healing potion can save if used immediately)\n\n");
        
        enhanced.append("Magic System:\n");
        enhanced.append("- Vancian casting: spells must be memorized daily\n");
        enhanced.append("- Clerics: divine spells, can turn undead\n");
        enhanced.append("- Magic-users: arcane spells, very limited armor\n");
        enhanced.append("- Saving throws: d20 vs. character save numbers\n\n");
        
        enhanced.append("Equipment Rules:\n");
        enhanced.append("- Rust monsters destroy metal on contact\n");
        enhanced.append("- Armor affects AC and movement\n");
        enhanced.append("- Weapon restrictions by class\n\n");
        
        enhanced.append("Alignment System:\n");
        enhanced.append("- Law: Protect others, honor commitments\n");
        enhanced.append("- Neutral: Balanced approach, survival focus\n");
        enhanced.append("- Chaos: Selfish, unpredictable behavior\n\n");
        
        // Add character-specific information
        if (context.has("character_state")) {
            enhanced.append("CHARACTER CAPABILITIES:\n");
            enhanced.append(buildCharacterCapabilities(context.get("character_state")));
        }
        
        // Add location-specific warnings
        if (context.has("current_location")) {
            enhanced.append("LOCATION HAZARDS:\n");
            enhanced.append(buildLocationHazards(context));
        }
        
        // Add known monster information
        if (context.has("known_monsters")) {
            enhanced.append("MONSTER ABILITIES:\n");
            enhanced.append(buildMonsterInfo(context.get("known_monsters")));
        }
        
        return enhanced.toString();
    }
    
    // Private validation methods
    
    private List<String> validateEquipmentRules(JsonNode action, JsonNode context) {
        List<String> violations = new ArrayList<>();
        
        if (action.has("item_use") || action.has("equipment_interaction")) {
            JsonNode interaction = action.has("item_use") ? action.get("item_use") : action.get("equipment_interaction");
            String item = interaction.path("item").asText();
            String location = context.path("current_location").asText();
            
            // Check for rust monster vulnerability
            if (isMetalItem(item) && isNearRustMonster(location, context)) {
                violations.add("Metal item '" + item + "' will be destroyed by rust monster");
            }
            
            // Check carrying capacity
            if (interaction.has("pickup") && exceedsCarryingCapacity(interaction, context)) {
                violations.add("Character cannot carry that much weight (check STR and encumbrance)");
            }
            
            // Check weapon proficiency
            if (interaction.has("attack_with") && !canUseWeapon(item, context)) {
                violations.add("Character class cannot use weapon: " + item);
            }
        }
        
        return violations;
    }
    
    private List<String> validateCombatRules(JsonNode action, JsonNode context) {
        List<String> violations = new ArrayList<>();
        
        if (action.has("combat_action")) {
            JsonNode combat = action.get("combat_action");
            
            // Validate attack sequence
            if (combat.has("attack") && !hasValidWeapon(combat, context)) {
                violations.add("Character lacks a usable weapon for attack");
            }
            
            // Validate range restrictions
            if (combat.has("missile_attack") && !isInRange(combat, context)) {
                violations.add("Target is out of weapon range");
            }
            
            // Validate spell casting in combat
            if (combat.has("cast_spell") && !canCastInCombat(combat, context)) {
                violations.add("Cannot cast spell in current combat situation");
            }
            
            // Check if character is conscious
            if (isCharacterUnconcious(context)) {
                violations.add("Character is unconscious and cannot take combat actions");
            }
        }
        
        return violations;
    }
    
    private List<String> validateMagicRules(JsonNode action, JsonNode context) {
        List<String> violations = new ArrayList<>();
        
        if (action.has("spell_casting")) {
            JsonNode spell = action.get("spell_casting");
            String spellName = spell.path("spell").asText();
            
            // Check if character can cast spells
            if (!canCastSpells(context)) {
                violations.add("Character class cannot cast spells");
            }
            
            // Check spell availability
            if (!hasSpellMemorized(spellName, context)) {
                violations.add("Spell '" + spellName + "' not memorized or already cast today");
            }
            
            // Check spell components and conditions
            if (!hasSpellComponents(spellName, context)) {
                violations.add("Missing required components for spell: " + spellName);
            }
            
            // Check target validity
            if (spell.has("target") && !isValidSpellTarget(spell.get("target"), spellName, context)) {
                violations.add("Invalid target for spell: " + spellName);
            }
        }
        
        return violations;
    }
    
    private List<String> validateAlignmentRules(JsonNode action, JsonNode context) {
        List<String> violations = new ArrayList<>();
        
        if (action.has("moral_choice") || action.has("character_behavior")) {
            JsonNode behavior = action.has("moral_choice") ? action.get("moral_choice") : action.get("character_behavior");
            String alignment = getCurrentAlignment(context);
            
            if (!actionMatchesAlignment(behavior, alignment)) {
                violations.add("Action contradicts " + alignment + " alignment: " + behavior.path("action").asText());
            }
        }
        
        return violations;
    }
    
    private List<String> validateCharacterCapabilities(JsonNode action, JsonNode context) {
        List<String> violations = new ArrayList<>();
        
        JsonNode character = context.path("character_state");
        String charClass = character.path("class").asText();
        int level = character.path("level").asInt(1);
        
        // Validate class-specific abilities
        if (action.has("special_ability")) {
            String ability = action.get("special_ability").path("ability").asText();
            
            if (!hasClassAbility(charClass, ability, level)) {
                violations.add("Character class '" + charClass + "' does not have ability: " + ability);
            }
        }
        
        // Validate thief skills
        if (action.has("thief_skill")) {
            if (!"thief".equalsIgnoreCase(charClass)) {
                violations.add("Only thieves can use thief skills");
            }
        }
        
        // Validate turn undead
        if (action.has("turn_undead")) {
            if (!"cleric".equalsIgnoreCase(charClass)) {
                violations.add("Only clerics can turn undead");
            }
        }
        
        return violations;
    }
    
    // Helper methods
    
    private JsonNode calculateAbilityModifiers(JsonNode character) {
        ObjectNode modifiers = objectMapper.createObjectNode();
        
        if (character.has("ability_scores")) {
            JsonNode scores = character.get("ability_scores");
            
            modifiers.put("strength_bonus", calculateAbilityBonus(scores.path("strength").asInt()));
            modifiers.put("dexterity_bonus", calculateAbilityBonus(scores.path("dexterity").asInt()));
            modifiers.put("constitution_bonus", calculateAbilityBonus(scores.path("constitution").asInt()));
            modifiers.put("intelligence_bonus", calculateAbilityBonus(scores.path("intelligence").asInt()));
            modifiers.put("wisdom_bonus", calculateAbilityBonus(scores.path("wisdom").asInt()));
            modifiers.put("charisma_bonus", calculateAbilityBonus(scores.path("charisma").asInt()));
        }
        
        return modifiers;
    }
    
    private int calculateAbilityBonus(int abilityScore) {
        if (abilityScore >= 16) return 2;
        if (abilityScore >= 13) return 1;
        if (abilityScore >= 9) return 0;
        if (abilityScore >= 6) return -1;
        return -2;
    }
    
    private JsonNode getSavingThrowTable(JsonNode character) {
        ObjectNode saves = objectMapper.createObjectNode();
        String charClass = character.path("class").asText().toLowerCase();
        int level = character.path("level").asInt(1);
        
        // TSR Basic D&D saving throws for 1st level characters
        switch (charClass) {
            case "fighter":
                saves.put("death_ray_poison", 12);
                saves.put("magic_wands", 13);
                saves.put("paralysis_stone", 14);
                saves.put("dragon_breath", 15);
                saves.put("spells", 16);
                break;
            case "cleric":
                saves.put("death_ray_poison", 11);
                saves.put("magic_wands", 12);
                saves.put("paralysis_stone", 14);
                saves.put("dragon_breath", 16);
                saves.put("spells", 15);
                break;
            case "magic-user":
                saves.put("death_ray_poison", 13);
                saves.put("magic_wands", 14);
                saves.put("paralysis_stone", 13);
                saves.put("dragon_breath", 16);
                saves.put("spells", 15);
                break;
            case "thief":
                saves.put("death_ray_poison", 13);
                saves.put("magic_wands", 14);
                saves.put("paralysis_stone", 13);
                saves.put("dragon_breath", 16);
                saves.put("spells", 15);
                break;
            default:
                // Default to fighter saves
                saves.put("death_ray_poison", 12);
                saves.put("magic_wands", 13);
                saves.put("paralysis_stone", 14);
                saves.put("dragon_breath", 15);
                saves.put("spells", 16);
        }
        
        return saves;
    }
    
    private JsonNode getCharacterCapabilities(JsonNode character) {
        ObjectNode capabilities = objectMapper.createObjectNode();
        String charClass = character.path("class").asText().toLowerCase();
        int level = character.path("level").asInt(1);
        
        ArrayNode abilities = capabilities.putArray("abilities");
        ArrayNode restrictions = capabilities.putArray("restrictions");
        
        switch (charClass) {
            case "fighter":
                abilities.add("Best combat abilities");
                abilities.add("Can use any armor and weapons");
                abilities.add("Multiple attacks at higher levels");
                restrictions.add("Cannot cast spells");
                break;
                
            case "cleric":
                abilities.add("Can cast divine spells");
                abilities.add("Can turn undead");
                abilities.add("Good combat abilities");
                restrictions.add("Cannot use edged weapons (swords, daggers)");
                restrictions.add("Limited to blunt weapons (maces, clubs)");
                break;
                
            case "magic-user":
                abilities.add("Can cast arcane spells");
                abilities.add("Highest spell progression");
                abilities.add("Can use magic items");
                restrictions.add("Very limited armor (no armor above AC 9)");
                restrictions.add("Limited weapon selection (daggers, staves)");
                restrictions.add("Spells must be memorized daily");
                break;
                
            case "thief":
                abilities.add("Can pick locks and find traps");
                abilities.add("Can backstab for extra damage");
                abilities.add("Can climb walls and move silently");
                abilities.add("Can hide in shadows");
                restrictions.add("Limited armor (leather only)");
                restrictions.add("Limited weapon selection");
                break;
        }
        
        return capabilities;
    }
    
    private JsonNode getKnownMonsters(JsonNode context) {
        ObjectNode monsters = objectMapper.createObjectNode();
        
        // Add TSR Basic monsters with their special abilities
        ObjectNode rustMonster = monsters.putObject("rust_monster");
        rustMonster.put("special_attack", "destroys_metal_on_contact");
        rustMonster.put("description", "Giant armadillo with feathery antennae");
        rustMonster.put("threat_level", "high_equipment_loss");
        
        ObjectNode ghouls = monsters.putObject("ghouls");
        ghouls.put("special_attack", "paralysis_on_hit");
        ghouls.put("weakness", "can_be_turned_by_clerics");
        ghouls.put("behavior", "undead_avoid_sunlight");
        
        ObjectNode skeletons = monsters.putObject("skeletons");
        skeletons.put("special_defense", "immune_to_sleep_charm");
        skeletons.put("weakness", "can_be_turned_by_clerics");
        skeletons.put("behavior", "mindless_undead");
        
        return monsters;
    }
    
    private JsonNode getEquipmentWarnings(JsonNode context) {
        ArrayNode warnings = objectMapper.createArrayNode();
        
        String location = context.path("current_location").asText();
        
        if ("rust_monster_chamber".equals(location)) {
            warnings.add("WARNING: Rust monster present - all metal items will be destroyed on contact!");
        }
        
        if (location.contains("ghoul")) {
            warnings.add("WARNING: Ghouls can paralyze on hit - clerics can turn them");
        }
        
        if (location.contains("trap")) {
            warnings.add("WARNING: Traps detected - thieves have better chance to detect/disable");
        }
        
        return warnings;
    }
    
    // Utility methods
    
    private boolean isMetalItem(String item) {
        return METAL_ITEMS.stream().anyMatch(metal -> item.toLowerCase().contains(metal));
    }
    
    private boolean isNearRustMonster(String location, JsonNode context) {
        return "rust_monster_chamber".equals(location) || 
               context.path("world_state").path(location).path("monsters").toString().contains("rust_monster");
    }
    
    private boolean exceedsCarryingCapacity(JsonNode interaction, JsonNode context) {
        // Simple carrying capacity check based on strength
        int strength = context.path("character_state").path("ability_scores").path("strength").asInt(10);
        int maxCarry = strength * 10; // Basic rule: STR * 10 in coins/items
        
        // This is a simplified check - full implementation would track actual weight
        return false; // Placeholder
    }
    
    private boolean canUseWeapon(String weapon, JsonNode context) {
        String charClass = context.path("character_state").path("class").asText().toLowerCase();
        
        switch (charClass) {
            case "fighter":
                return true; // Fighters can use any weapon
            case "cleric":
                return !weapon.toLowerCase().contains("sword") && !weapon.toLowerCase().contains("dagger");
            case "magic-user":
                return weapon.toLowerCase().contains("dagger") || weapon.toLowerCase().contains("staff");
            case "thief":
                return FIGHTER_WEAPONS.contains(weapon.toLowerCase());
            default:
                return false;
        }
    }
    
    private boolean hasValidWeapon(JsonNode combat, JsonNode context) {
        // Check if character has a weapon equipped
        JsonNode equipment = context.path("character_state").path("equipment");
        return equipment.has("weapons") && equipment.get("weapons").size() > 0;
    }
    
    private boolean isInRange(JsonNode combat, JsonNode context) {
        // Simplified range check - full implementation would check actual distances
        return true; // Placeholder
    }
    
    private boolean canCastInCombat(JsonNode combat, JsonNode context) {
        // Check if spell casting is interrupted
        return !context.path("character_state").path("status_effects").toString().contains("interrupted");
    }
    
    private boolean isCharacterUnconcious(JsonNode context) {
        int hp = context.path("character_state").path("current_hp").asInt(1);
        return hp <= 0;
    }
    
    private boolean canCastSpells(JsonNode context) {
        String charClass = context.path("character_state").path("class").asText().toLowerCase();
        return "cleric".equals(charClass) || "magic-user".equals(charClass);
    }
    
    private boolean hasSpellMemorized(String spellName, JsonNode context) {
        JsonNode spells = context.path("character_state").path("memorized_spells");
        if (spells.isArray()) {
            for (JsonNode spell : spells) {
                if (spellName.equals(spell.asText())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean hasSpellComponents(String spellName, JsonNode context) {
        // Most basic spells don't require material components
        return true; // Placeholder
    }
    
    private boolean isValidSpellTarget(JsonNode target, String spellName, JsonNode context) {
        // Basic target validation
        return true; // Placeholder
    }
    
    private String getCurrentAlignment(JsonNode context) {
        return context.path("character_state").path("alignment").asText("lawful");
    }
    
    private boolean actionMatchesAlignment(JsonNode behavior, String alignment) {
        String action = behavior.path("action").asText().toLowerCase();
        
        switch (alignment.toLowerCase()) {
            case "lawful":
                return !action.contains("steal") && !action.contains("murder") && !action.contains("betray");
            case "chaotic":
                return !action.contains("help") || action.contains("self");
            case "neutral":
                return true; // Neutral can do most things
            default:
                return true;
        }
    }
    
    private boolean hasClassAbility(String charClass, String ability, int level) {
        // Check if character class has the specified ability at current level
        switch (charClass.toLowerCase()) {
            case "cleric":
                return "turn_undead".equals(ability) || "cast_spell".equals(ability);
            case "magic-user":
                return "cast_spell".equals(ability);
            case "thief":
                return ability.startsWith("thief_");
            case "fighter":
                return "extra_attack".equals(ability) && level >= 4;
            default:
                return false;
        }
    }
    
    private List<String> generateDnDSuggestions(JsonNode action, List<String> violations, JsonNode context) {
        List<String> suggestions = new ArrayList<>();
        
        for (String violation : violations) {
            if (violation.contains("rust monster")) {
                suggestions.add("Consider using non-metal weapons or avoiding the rust monster");
                suggestions.add("Gems and coins might survive even if metal is destroyed");
            }
            
            if (violation.contains("weapon")) {
                suggestions.add("Check your class weapon restrictions in character capabilities");
                suggestions.add("Fighters can use any weapon, clerics avoid edged weapons");
            }
            
            if (violation.contains("spell")) {
                suggestions.add("Make sure you have the spell memorized for today");
                suggestions.add("Clerics get spells from meditation, magic-users from spell books");
            }
            
            if (violation.contains("alignment")) {
                suggestions.add("Consider actions that match your character's moral code");
                suggestions.add("Lawful characters protect others, Chaotic characters act selfishly");
            }
        }
        
        return suggestions;
    }
    
    // Event handlers
    
    private void handleRustMonsterAttack(JsonNode changes, JsonNode context) {
        LOGGER.info("Handling rust monster attack - destroying metal equipment");
        // Implementation would remove metal items from character inventory
    }
    
    private void handleCharacterDeath(JsonNode changes, JsonNode context) {
        LOGGER.info("Handling character death - checking for healing potion");
        // Implementation would check for available healing options
    }
    
    private void handleSpellCasting(JsonNode changes, JsonNode context) {
        LOGGER.info("Handling spell casting - removing from memorized spells");
        // Implementation would remove cast spell from available spells
    }
    
    private void handleCombatDamage(JsonNode changes, JsonNode context) {
        LOGGER.info("Handling combat damage - updating character HP");
        // Implementation would apply damage to character
    }
    
    private void calculateExperiencePoints(JsonNode changes, JsonNode context) {
        LOGGER.info("Calculating experience points for treasure found");
        // Implementation would add XP equal to treasure value
    }
    
    private void awardCombatExperience(JsonNode changes, JsonNode context) {
        LOGGER.info("Awarding combat experience for monster defeated");
        // Implementation would add XP for defeated monsters
    }
    
    private void checkLevelAdvancement(JsonNode changes, JsonNode context) {
        LOGGER.info("Checking for level advancement");
        // Implementation would check if character has enough XP to advance
    }
    
    private void updateArmorClass(JsonNode changes, JsonNode context) {
        LOGGER.info("Updating armor class due to equipment changes");
        // Implementation would recalculate AC based on remaining equipment
    }
    
    private String buildCharacterCapabilities(JsonNode character) {
        StringBuilder caps = new StringBuilder();
        String charClass = character.path("class").asText();
        int level = character.path("level").asInt(1);
        
        caps.append("Class: ").append(charClass).append(" (Level ").append(level).append(")\n");
        
        JsonNode abilities = getCharacterCapabilities(character);
        if (abilities.has("abilities")) {
            for (JsonNode ability : abilities.get("abilities")) {
                caps.append("- ").append(ability.asText()).append("\n");
            }
        }
        
        if (abilities.has("restrictions")) {
            caps.append("Restrictions:\n");
            for (JsonNode restriction : abilities.get("restrictions")) {
                caps.append("- ").append(restriction.asText()).append("\n");
            }
        }
        
        return caps.toString();
    }
    
    private String buildLocationHazards(JsonNode context) {
        StringBuilder hazards = new StringBuilder();
        String location = context.path("current_location").asText();
        
        if (location.contains("rust_monster")) {
            hazards.append("- EXTREME: Rust monster will destroy all metal items\n");
        }
        
        if (location.contains("ghoul")) {
            hazards.append("- HIGH: Ghouls can paralyze with touch\n");
        }
        
        if (location.contains("trap")) {
            hazards.append("- MEDIUM: Mechanical traps may be present\n");
        }
        
        if (location.contains("poison")) {
            hazards.append("- HIGH: Poisonous creatures or traps\n");
        }
        
        return hazards.toString();
    }
    
    private String buildMonsterInfo(JsonNode monsters) {
        StringBuilder info = new StringBuilder();
        
        monsters.fields().forEachRemaining(entry -> {
            String monsterName = entry.getKey().replace("_", " ");
            JsonNode monsterData = entry.getValue();
            
            info.append("- ").append(monsterName.toUpperCase()).append(": ");
            
            if (monsterData.has("special_attack")) {
                info.append(monsterData.get("special_attack").asText()).append("; ");
            }
            
            if (monsterData.has("weakness")) {
                info.append("Weakness: ").append(monsterData.get("weakness").asText());
            }
            
            info.append("\n");
        });
        
        return info.toString();
    }
}
