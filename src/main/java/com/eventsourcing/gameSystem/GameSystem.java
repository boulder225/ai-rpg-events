package com.eventsourcing.gameSystem;

import java.util.List;
import java.util.Map;

/**
 * Generic RPG system interface that can support any tabletop RPG.
 * Today: D&D Basic Rules. Tomorrow: Call of Cthulhu, Vampire, etc.
 */
public interface GameSystem {
    
    String getSystemName();
    String getSystemVersion();
    
    // Core mechanics
    Map<String, Object> getSystemRules();
    List<String> getCoreAttributes();
    List<String> getAvailableSkills();
    
    // Character creation
    Map<String, Object> createCharacter(String name, Map<String, Object> options);
    boolean validateCharacter(Map<String, Object> character);
    
    // Dice mechanics
    DiceResult rollDice(String diceExpression);
    DiceResult makeSkillCheck(String skill, Map<String, Object> character, int difficulty);
    DiceResult makeSavingThrow(String saveType, Map<String, Object> character, int difficulty);
    
    // Combat
    int calculateInitiative(Map<String, Object> character);
    int calculateArmorClass(Map<String, Object> character);
    int calculateHitPoints(Map<String, Object> character);
    DiceResult makeAttack(Map<String, Object> attacker, Map<String, Object> target);
    
    // Adventure integration
    String getAdventureContext();
    Map<String, Object> processAdventureChoice(String choice, Map<String, Object> gameState);
    
    // AI context generation
    String generateSystemContext(Map<String, Object> character, Map<String, Object> gameState);
    
    record DiceResult(
        int total,
        List<Integer> rolls,
        String expression,
        boolean success,
        String description
    ) {}
}
