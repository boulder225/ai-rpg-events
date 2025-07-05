package com.eventsourcing.dnd;

import java.util.List;
import java.util.Map;

/**
 * D&D 5e Core Rules and Game Mechanics.
 * Provides authentic D&D gameplay structure for the AI system.
 */
public class DnDRules {
    
    // Core Abilities
    public enum Ability {
        STRENGTH, DEXTERITY, CONSTITUTION, INTELLIGENCE, WISDOM, CHARISMA
    }
    
    // Character Classes (simplified)
    public enum CharacterClass {
        FIGHTER("Warrior trained in combat", List.of("Athletics", "Intimidation")),
        WIZARD("Master of arcane magic", List.of("Arcana", "Investigation")),
        ROGUE("Skilled in stealth and cunning", List.of("Stealth", "Sleight of Hand")),
        CLERIC("Divine spellcaster and healer", List.of("Medicine", "Religion")),
        RANGER("Nature warrior and tracker", List.of("Survival", "Animal Handling")),
        BARD("Charismatic performer and jack-of-all-trades", List.of("Performance", "Persuasion"));
        
        private final String description;
        private final List<String> skills;
        
        CharacterClass(String description, List<String> skills) {
            this.description = description;
            this.skills = skills;
        }
        
        public String getDescription() { return description; }
        public List<String> getSkills() { return skills; }
    }
    
    // Character Races
    public enum Race {
        HUMAN("Versatile and ambitious", Map.of()),
        ELF("Graceful and magical", Map.of(Ability.DEXTERITY, 2)),
        DWARF("Hardy and resilient", Map.of(Ability.CONSTITUTION, 2)),
        HALFLING("Small but brave", Map.of(Ability.DEXTERITY, 2)),
        DRAGONBORN("Draconic heritage", Map.of(Ability.STRENGTH, 2)),
        GNOME("Small and clever", Map.of(Ability.INTELLIGENCE, 2));
        
        private final String description;
        private final Map<Ability, Integer> abilityBonuses;
        
        Race(String description, Map<Ability, Integer> abilityBonuses) {
            this.description = description;
            this.abilityBonuses = abilityBonuses;
        }
        
        public String getDescription() { return description; }
        public Map<Ability, Integer> getAbilityBonuses() { return abilityBonuses; }
    }
    
    // Core Skills
    public static final List<String> SKILLS = List.of(
        "Acrobatics", "Animal Handling", "Arcana", "Athletics", "Deception",
        "History", "Insight", "Intimidation", "Investigation", "Medicine",
        "Nature", "Perception", "Performance", "Persuasion", "Religion",
        "Sleight of Hand", "Stealth", "Survival"
    );
    
    // Dice Rolling
    public static class DiceRoll {
        public static int roll(int sides) {
            return 1 + (int)(Math.random() * sides);
        }
        
        public static int rollD20() { return roll(20); }
        public static int rollD12() { return roll(12); }
        public static int rollD10() { return roll(10); }
        public static int rollD8() { return roll(8); }
        public static int rollD6() { return roll(6); }
        public static int rollD4() { return roll(4); }
        
        public static int rollWithAdvantage() {
            return Math.max(rollD20(), rollD20());
        }
        
        public static int rollWithDisadvantage() {
            return Math.min(rollD20(), rollD20());
        }
        
        public static int rollAbilityScore() {
            // Roll 4d6, drop lowest
            int[] rolls = {rollD6(), rollD6(), rollD6(), rollD6()};
            java.util.Arrays.sort(rolls);
            return rolls[1] + rolls[2] + rolls[3]; // Sum highest 3
        }
    }
    
    // Difficulty Classes
    public static class DC {
        public static final int VERY_EASY = 5;
        public static final int EASY = 10;
        public static final int MEDIUM = 15;
        public static final int HARD = 20;
        public static final int VERY_HARD = 25;
        public static final int NEARLY_IMPOSSIBLE = 30;
    }
    
    // Combat Rules
    public static class Combat {
        public static int calculateAC(int baseAC, int dexModifier) {
            return baseAC + dexModifier;
        }
        
        public static int calculateHitPoints(CharacterClass charClass, int constitution, int level) {
            int hitDie = switch (charClass) {
                case FIGHTER -> 10;
                case WIZARD -> 6;
                case ROGUE -> 8;
                case CLERIC -> 8;
                case RANGER -> 10;
                case BARD -> 8;
            };
            
            int constitutionModifier = getAbilityModifier(constitution);
            return hitDie + constitutionModifier + ((level - 1) * (hitDie/2 + 1 + constitutionModifier));
        }
    }
    
    // Utility Methods
    public static int getAbilityModifier(int abilityScore) {
        return (abilityScore - 10) / 2;
    }
    
    public static String getProficiencyBonus(int level) {
        return String.valueOf(Math.max(2, (level - 1) / 4 + 2));
    }
    
    public static boolean makeSkillCheck(int abilityScore, int proficiencyBonus, int dc) {
        int roll = DiceRoll.rollD20();
        int modifier = getAbilityModifier(abilityScore);
        int total = roll + modifier + proficiencyBonus;
        return total >= dc;
    }
    
    // Spell Levels
    public static final Map<Integer, String> SPELL_LEVELS = Map.of(
        0, "Cantrip",
        1, "1st Level",
        2, "2nd Level", 
        3, "3rd Level",
        4, "4th Level",
        5, "5th Level",
        6, "6th Level",
        7, "7th Level",
        8, "8th Level",
        9, "9th Level"
    );
}
