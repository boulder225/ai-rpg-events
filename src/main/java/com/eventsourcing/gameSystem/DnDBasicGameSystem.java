package com.eventsourcing.gameSystem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * D&D Basic Rules implementation of the generic game system.
 * Loads rules and adventure from src/resources/dnd/
 */
public class DnDBasicGameSystem implements GameSystem {
    
    private final String rulesContent;
    private final String adventureContent;
    private final Random random = new Random();
    
    public DnDBasicGameSystem() {
        this.rulesContent = loadResourceFile("dnd/basic_rules.txt");
        this.adventureContent = loadResourceFile("dnd/solo_adventure.txt");
    }
    
    @Override
    public String getSystemName() {
        return "D&D Basic Rules";
    }
    
    @Override
    public String getSystemVersion() {
        return "TSR 1011";
    }
    
    @Override
    public Map<String, Object> getSystemRules() {
        return Map.of(
            "dice_system", "d20 + modifiers",
            "core_mechanic", "Roll high against target number",
            "initiative", "d6 + Dexterity modifier",
            "armor_class", "Ascending AC (higher is better)",
            "hit_points", "Class hit die + Constitution modifier per level",
            "saving_throws", Map.of(
                "Death Ray or Poison", "roll vs death",
                "Magic Wands", "roll vs wands", 
                "Paralysis or Turn to Stone", "roll vs paralysis",
                "Dragon Breath", "roll vs breath",
                "Rods, Staves, or Spells", "roll vs spells"
            )
        );
    }
    
    @Override
    public List<String> getCoreAttributes() {
        return List.of("Strength", "Intelligence", "Wisdom", "Dexterity", "Constitution", "Charisma");
    }
    
    @Override
    public List<String> getAvailableSkills() {
        return List.of(
            "Climb Sheer Surfaces", "Find or Remove Traps", "Hear Noise",
            "Hide in Shadows", "Move Silently", "Open Locks", "Pick Pockets"
        );
    }
    
    @Override
    public Map<String, Object> createCharacter(String name, Map<String, Object> options) {
        var character = new HashMap<String, Object>();
        character.put("name", name);
        character.put("level", 1);
        character.put("experience", 0);
        
        // Roll ability scores (3d6 for each)
        var abilities = new HashMap<String, Integer>();
        for (String ability : getCoreAttributes()) {
            abilities.put(ability, rollAbilityScore());
        }
        character.put("abilities", abilities);
        
        // Choose class (default to Fighter if not specified)
        String characterClass = (String) options.getOrDefault("class", "Fighter");
        character.put("class", characterClass);
        
        // Calculate derived stats
        character.put("hit_points", calculateHitPoints(character));
        character.put("armor_class", calculateArmorClass(character));
        
        // Starting equipment based on class
        character.put("equipment", getStartingEquipment(characterClass));
        
        // Saving throws
        character.put("saving_throws", getSavingThrows(characterClass, 1));
        
        return character;
    }
    
    @Override
    public boolean validateCharacter(Map<String, Object> character) {
        // Basic validation - ensure required fields exist
        return character.containsKey("name") && 
               character.containsKey("class") && 
               character.containsKey("abilities") &&
               character.containsKey("hit_points");
    }
    
    @Override
    public DiceResult rollDice(String diceExpression) {
        // Parse expressions like "3d6", "1d20+2", "2d8-1"
        try {
            if (diceExpression.equals("1d20")) {
                int roll = 1 + random.nextInt(20);
                return new DiceResult(roll, List.of(roll), diceExpression, true, 
                    "Rolled " + roll + " on d20");
            } else if (diceExpression.equals("3d6")) {
                var rolls = List.of(
                    1 + random.nextInt(6),
                    1 + random.nextInt(6), 
                    1 + random.nextInt(6)
                );
                int total = rolls.stream().mapToInt(Integer::intValue).sum();
                return new DiceResult(total, rolls, diceExpression, true,
                    "Rolled " + rolls + " = " + total);
            } else if (diceExpression.equals("1d6")) {
                int roll = 1 + random.nextInt(6);
                return new DiceResult(roll, List.of(roll), diceExpression, true,
                    "Rolled " + roll + " on d6");
            }
        } catch (Exception e) {
            // Fallback for complex expressions
        }
        
        // Default fallback
        int roll = 1 + random.nextInt(20);
        return new DiceResult(roll, List.of(roll), "1d20", true, "Fallback roll: " + roll);
    }
    
    @Override
    public DiceResult makeSkillCheck(String skill, Map<String, Object> character, int difficulty) {
        var diceResult = rollDice("1d20");
        
        // Apply ability modifier based on skill
        int modifier = getSkillModifier(skill, character);
        int total = diceResult.total() + modifier;
        boolean success = total >= difficulty;
        
        return new DiceResult(
            total,
            diceResult.rolls(),
            "1d20+" + modifier,
            success,
            skill + " check: " + total + " vs " + difficulty + " = " + (success ? "SUCCESS" : "FAILURE")
        );
    }
    
    @Override
    public DiceResult makeSavingThrow(String saveType, Map<String, Object> character, int difficulty) {
        var diceResult = rollDice("1d20");
        
        // Get saving throw bonus from character
        @SuppressWarnings("unchecked")
        var savingThrows = (Map<String, Integer>) character.get("saving_throws");
        int saveBonus = savingThrows.getOrDefault(saveType, 0);
        
        int total = diceResult.total() + saveBonus;
        boolean success = total >= difficulty;
        
        return new DiceResult(
            total,
            diceResult.rolls(),
            "1d20+" + saveBonus,
            success,
            saveType + " save: " + total + " vs " + difficulty + " = " + (success ? "SUCCESS" : "FAILURE")
        );
    }
    
    @Override
    public int calculateInitiative(Map<String, Object> character) {
        @SuppressWarnings("unchecked")
        var abilities = (Map<String, Integer>) character.get("abilities");
        int dexModifier = getAbilityModifier(abilities.get("Dexterity"));
        return rollDice("1d6").total() + dexModifier;
    }
    
    @Override
    public int calculateArmorClass(Map<String, Object> character) {
        @SuppressWarnings("unchecked")
        var abilities = (Map<String, Integer>) character.get("abilities");
        int dexModifier = getAbilityModifier(abilities.get("Dexterity"));
        
        // Base AC 10 + Dex modifier (assuming no armor for now)
        return 10 + dexModifier;
    }
    
    @Override
    public int calculateHitPoints(Map<String, Object> character) {
        String characterClass = (String) character.get("class");
        @SuppressWarnings("unchecked")
        var abilities = (Map<String, Integer>) character.get("abilities");
        int level = (Integer) character.getOrDefault("level", 1);
        
        int hitDie = getClassHitDie(characterClass);
        int conModifier = getAbilityModifier(abilities.get("Constitution"));
        
        // Level 1: max hit die + con modifier
        // Higher levels: average of hit die + con modifier per level
        if (level == 1) {
            return hitDie + conModifier;
        } else {
            return hitDie + conModifier + ((level - 1) * ((hitDie / 2 + 1) + conModifier));
        }
    }
    
    @Override
    public DiceResult makeAttack(Map<String, Object> attacker, Map<String, Object> target) {
        var attackRoll = rollDice("1d20");
        
        // Calculate attack bonus
        @SuppressWarnings("unchecked")
        var abilities = (Map<String, Integer>) attacker.get("abilities");
        int strModifier = getAbilityModifier(abilities.get("Strength"));
        int level = (Integer) attacker.getOrDefault("level", 1);
        
        int attackBonus = strModifier + level; // Simplified attack bonus
        int totalAttack = attackRoll.total() + attackBonus;
        
        int targetAC = calculateArmorClass(target);
        boolean hit = totalAttack >= targetAC;
        
        return new DiceResult(
            totalAttack,
            attackRoll.rolls(),
            "1d20+" + attackBonus,
            hit,
            "Attack roll: " + totalAttack + " vs AC " + targetAC + " = " + (hit ? "HIT" : "MISS")
        );
    }
    
    @Override
    public String getAdventureContext() {
        var context = new StringBuilder();
        context.append("D&D BASIC RULES SOLO ADVENTURE\n");
        context.append("===============================\n\n");
        
        context.append("SYSTEM: ").append(getSystemName()).append(" (").append(getSystemVersion()).append(")\n\n");
        
        if (rulesContent != null && !rulesContent.trim().isEmpty()) {
            context.append("CORE RULES:\n");
            context.append(rulesContent.length() > 1000 ? 
                rulesContent.substring(0, 1000) + "..." : rulesContent);
            context.append("\n\n");
        }
        
        if (adventureContent != null && !adventureContent.trim().isEmpty()) {
            context.append("SOLO ADVENTURE:\n");
            context.append(adventureContent.length() > 2000 ? 
                adventureContent.substring(0, 2000) + "..." : adventureContent);
            context.append("\n\n");
        }
        
        return context.toString();
    }
    
    @Override
    public Map<String, Object> processAdventureChoice(String choice, Map<String, Object> gameState) {
        var newState = new HashMap<>(gameState);
        
        // Process choice and update game state
        // This would parse the adventure text and determine outcomes
        newState.put("last_choice", choice);
        newState.put("choice_timestamp", System.currentTimeMillis());
        
        return newState;
    }
    
    @Override
    public String generateSystemContext(Map<String, Object> character, Map<String, Object> gameState) {
        var context = new StringBuilder();
        
        context.append("CHARACTER CONTEXT:\n");
        context.append("Name: ").append(character.get("name")).append("\n");
        context.append("Class: ").append(character.get("class")).append("\n");
        context.append("Level: ").append(character.get("level")).append("\n");
        context.append("Hit Points: ").append(character.get("hit_points")).append("\n");
        context.append("Armor Class: ").append(character.get("armor_class")).append("\n");
        
        @SuppressWarnings("unchecked")
        var abilities = (Map<String, Integer>) character.get("abilities");
        context.append("Abilities: ");
        abilities.forEach((ability, score) -> 
            context.append(ability).append(" ").append(score).append(" "));
        context.append("\n\n");
        
        context.append("GAME STATE:\n");
        gameState.forEach((key, value) -> 
            context.append(key).append(": ").append(value).append("\n"));
        
        return context.toString();
    }
    
    // Helper methods
    private String loadResourceFile(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("Warning: Could not find resource: " + resourcePath);
                return "";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Error loading resource " + resourcePath + ": " + e.getMessage());
            return "";
        }
    }
    
    private int rollAbilityScore() {
        return rollDice("3d6").total();
    }
    
    private int getAbilityModifier(int abilityScore) {
        if (abilityScore <= 3) return -3;
        if (abilityScore <= 5) return -2;
        if (abilityScore <= 8) return -1;
        if (abilityScore <= 12) return 0;
        if (abilityScore <= 15) return 1;
        if (abilityScore <= 17) return 2;
        return 3;
    }
    
    private int getClassHitDie(String characterClass) {
        return switch (characterClass.toLowerCase()) {
            case "fighter" -> 8;
            case "cleric" -> 6;
            case "magic-user", "wizard" -> 4;
            case "thief" -> 6;
            case "elf" -> 6;
            case "dwarf" -> 8;
            case "halfling" -> 6;
            default -> 6;
        };
    }
    
    private List<String> getStartingEquipment(String characterClass) {
        return switch (characterClass.toLowerCase()) {
            case "fighter" -> List.of("Sword", "Shield", "Leather Armor", "50 gold pieces");
            case "cleric" -> List.of("Mace", "Shield", "Chain Mail", "Holy Symbol", "30 gold pieces");
            case "magic-user", "wizard" -> List.of("Dagger", "Spell Book", "30 gold pieces");
            case "thief" -> List.of("Sword", "Leather Armor", "Thieves' Tools", "30 gold pieces");
            default -> List.of("Dagger", "Clothes", "20 gold pieces");
        };
    }
    
    private Map<String, Integer> getSavingThrows(String characterClass, int level) {
        // Simplified saving throws for level 1 characters
        return switch (characterClass.toLowerCase()) {
            case "fighter" -> Map.of(
                "Death Ray or Poison", 12,
                "Magic Wands", 13,
                "Paralysis or Turn to Stone", 14,
                "Dragon Breath", 15,
                "Rods, Staves, or Spells", 16
            );
            case "cleric" -> Map.of(
                "Death Ray or Poison", 11,
                "Magic Wands", 12,
                "Paralysis or Turn to Stone", 14,
                "Dragon Breath", 16,
                "Rods, Staves, or Spells", 15
            );
            case "magic-user", "wizard" -> Map.of(
                "Death Ray or Poison", 13,
                "Magic Wands", 14,
                "Paralysis or Turn to Stone", 13,
                "Dragon Breath", 16,
                "Rods, Staves, or Spells", 15
            );
            case "thief" -> Map.of(
                "Death Ray or Poison", 13,
                "Magic Wands", 14,
                "Paralysis or Turn to Stone", 13,
                "Dragon Breath", 16,
                "Rods, Staves, or Spells", 15
            );
            default -> Map.of(
                "Death Ray or Poison", 14,
                "Magic Wands", 15,
                "Paralysis or Turn to Stone", 16,
                "Dragon Breath", 17,
                "Rods, Staves, or Spells", 18
            );
        };
    }
    
    private int getSkillModifier(String skill, Map<String, Object> character) {
        @SuppressWarnings("unchecked")
        var abilities = (Map<String, Integer>) character.get("abilities");
        
        // Map skills to appropriate abilities
        return switch (skill.toLowerCase()) {
            case "climb sheer surfaces" -> getAbilityModifier(abilities.get("Strength"));
            case "find or remove traps" -> getAbilityModifier(abilities.get("Intelligence"));
            case "hear noise" -> getAbilityModifier(abilities.get("Wisdom"));
            case "hide in shadows", "move silently" -> getAbilityModifier(abilities.get("Dexterity"));
            case "open locks", "pick pockets" -> getAbilityModifier(abilities.get("Dexterity"));
            default -> 0;
        };
    }
}
