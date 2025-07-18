package com.eventsourcing.gameSystem;

import java.util.List;
import java.util.Map;

public class DnDBasicGameSystem implements GameSystem {
    @Override
    public String getSystemName() { return "D&D Basic"; }

    @Override
    public String getSystemVersion() { return "1.0"; }

    @Override
    public Map<String, Object> getSystemRules() { return Map.of(); }

    @Override
    public List<String> getCoreAttributes() { return List.of(); }

    @Override
    public List<String> getAvailableSkills() { return List.of(); }

    @Override
    public Map<String, Object> createCharacter(String name, Map<String, Object> options) { return Map.of(); }

    @Override
    public boolean validateCharacter(Map<String, Object> character) { return true; }

    @Override
    public DiceResult rollDice(String diceExpression) { return new DiceResult(0, List.of(), diceExpression, false, "Stub"); }

    @Override
    public DiceResult makeSkillCheck(String skill, Map<String, Object> character, int difficulty) { return new DiceResult(0, List.of(), skill, false, "Stub"); }

    @Override
    public DiceResult makeSavingThrow(String saveType, Map<String, Object> character, int difficulty) { return new DiceResult(0, List.of(), saveType, false, "Stub"); }

    @Override
    public int calculateInitiative(Map<String, Object> character) { return 0; }

    @Override
    public int calculateArmorClass(Map<String, Object> character) { return 0; }

    @Override
    public int calculateHitPoints(Map<String, Object> character) { return 0; }

    @Override
    public DiceResult makeAttack(Map<String, Object> attacker, Map<String, Object> target) { return new DiceResult(0, List.of(), "attack", false, "Stub"); }

    @Override
    public String getAdventureContext() { return "Stub adventure context"; }

    @Override
    public Map<String, Object> processAdventureChoice(String choice, Map<String, Object> gameState) { return Map.of(); }

    @Override
    public String generateSystemContext(Map<String, Object> character, Map<String, Object> gameState) { return "Stub system context"; }
}
