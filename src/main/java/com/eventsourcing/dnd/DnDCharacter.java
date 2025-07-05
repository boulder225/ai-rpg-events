package com.eventsourcing.dnd;

import java.util.List;
import java.util.Map;

/**
 * D&D Character Sheet representation.
 * Integrates with event sourcing to track character progression.
 */
public record DnDCharacter(
    String characterId,
    String name,
    DnDRules.Race race,
    DnDRules.CharacterClass characterClass,
    int level,
    Map<DnDRules.Ability, Integer> abilityScores,
    List<String> skills,
    List<String> equipment,
    List<String> spells,
    int currentHitPoints,
    int maxHitPoints,
    int armorClass,
    String background,
    String alignment,
    Map<String, Object> features
) {
    
    public int getAbilityModifier(DnDRules.Ability ability) {
        return DnDRules.getAbilityModifier(abilityScores.get(ability));
    }
    
    public int getProficiencyBonus() {
        return Integer.parseInt(DnDRules.getProficiencyBonus(level));
    }
    
    public boolean isSkillProficient(String skill) {
        return skills.contains(skill) || characterClass.getSkills().contains(skill);
    }
    
    public int getSkillModifier(String skill, DnDRules.Ability ability) {
        int abilityMod = getAbilityModifier(ability);
        int profBonus = isSkillProficient(skill) ? getProficiencyBonus() : 0;
        return abilityMod + profBonus;
    }
    
    public String getCharacterSummary() {
        return String.format(
            "%s - Level %d %s %s (AC: %d, HP: %d/%d)",
            name, level, race.name(), characterClass.name(), 
            armorClass, currentHitPoints, maxHitPoints
        );
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String characterId;
        private String name;
        private DnDRules.Race race;
        private DnDRules.CharacterClass characterClass;
        private int level = 1;
        private Map<DnDRules.Ability, Integer> abilityScores;
        private List<String> skills;
        private List<String> equipment;
        private List<String> spells = List.of();
        private int currentHitPoints;
        private int maxHitPoints;
        private int armorClass = 10;
        private String background = "Folk Hero";
        private String alignment = "Neutral Good";
        private Map<String, Object> features = Map.of();
        
        public Builder characterId(String characterId) { this.characterId = characterId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder race(DnDRules.Race race) { this.race = race; return this; }
        public Builder characterClass(DnDRules.CharacterClass characterClass) { 
            this.characterClass = characterClass; 
            this.skills = characterClass.getSkills();
            return this; 
        }
        public Builder level(int level) { this.level = level; return this; }
        public Builder abilityScores(Map<DnDRules.Ability, Integer> abilityScores) { 
            this.abilityScores = abilityScores; 
            // Calculate HP based on class and constitution
            int constitution = abilityScores.get(DnDRules.Ability.CONSTITUTION);
            this.maxHitPoints = DnDRules.Combat.calculateHitPoints(characterClass, constitution, level);
            this.currentHitPoints = maxHitPoints;
            
            // Calculate AC (base 10 + DEX modifier)
            int dexterity = abilityScores.get(DnDRules.Ability.DEXTERITY);
            this.armorClass = 10 + DnDRules.getAbilityModifier(dexterity);
            
            return this; 
        }
        public Builder equipment(List<String> equipment) { this.equipment = equipment; return this; }
        public Builder background(String background) { this.background = background; return this; }
        public Builder alignment(String alignment) { this.alignment = alignment; return this; }
        
        public DnDCharacter build() {
            return new DnDCharacter(
                characterId, name, race, characterClass, level,
                abilityScores, skills, equipment, spells,
                currentHitPoints, maxHitPoints, armorClass,
                background, alignment, features
            );
        }
    }
}
