package com.eventsourcing.rpg;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Business logic for processing RPG commands (KISS version).
 * Contains the core domain rules and validations.
 */
public class RPGBusinessLogic {
    // Create a new player state with basic starting equipment
    public static RPGState.PlayerState createPlayer(String playerId, String name) {
        // Starting equipment for new D&D Basic character
        Map<String, String> startingEquipment = Map.of(
            "weapon", "sword",
            "armor", "chain_mail"
        );
        
        List<String> startingInventory = List.of(
            "torch", "rations", "rope", "dagger"
        );
        
        return new RPGState.PlayerState(
            playerId,
            name,
            "village",
            100,
            Map.of(),
            Map.of(),
            List.of(),
            List.of(),
            List.of(),
            startingEquipment,
            startingInventory,
            Instant.now()
        );
    }

    // Move player to a new location
    public static RPGState.PlayerState movePlayer(RPGState.PlayerState state, String toLocationId) {
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            toLocationId,
            state.health(),
            state.skills(),
            state.relationships(),
            state.completedQuests(),
            state.activeQuests(),
            state.actionHistory(),
            state.equipment(),
            state.inventory(),
            Instant.now()
        );
    }

    // Change player health
    public static RPGState.PlayerState changePlayerHealth(RPGState.PlayerState state, int newHealth) {
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            state.currentLocationId(),
            newHealth,
            state.skills(),
            state.relationships(),
            state.completedQuests(),
            state.activeQuests(),
            state.actionHistory(),
            state.equipment(),
            state.inventory(),
            Instant.now()
        );
    }

    // Add a skill to player
    public static RPGState.PlayerState addPlayerSkill(RPGState.PlayerState state, String skillName, int level) {
        var newSkills = new java.util.HashMap<>(state.skills());
        newSkills.put(skillName, level);
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            state.currentLocationId(),
            state.health(),
            newSkills,
            state.relationships(),
            state.completedQuests(),
            state.activeQuests(),
            state.actionHistory(),
            state.equipment(),
            state.inventory(),
            Instant.now()
        );
    }

    // Add a relationship
    public static RPGState.PlayerState addRelationship(RPGState.PlayerState state, String npcId, String relationType) {
        var newRelationships = new java.util.HashMap<>(state.relationships());
        newRelationships.put(npcId, new RPGState.Relationship(
            npcId, relationType, 50, 50, List.of(), Instant.now()
        ));
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            state.currentLocationId(),
            state.health(),
            state.skills(),
            newRelationships,
            state.completedQuests(),
            state.activeQuests(),
            state.actionHistory(),
            state.equipment(),
            state.inventory(),
            Instant.now()
        );
    }

    // Start a quest
    public static RPGState.PlayerState startQuest(RPGState.PlayerState state, String questId) {
        var newActiveQuests = new java.util.ArrayList<>(state.activeQuests());
        if (!newActiveQuests.contains(questId)) newActiveQuests.add(questId);
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            state.currentLocationId(),
            state.health(),
            state.skills(),
            state.relationships(),
            state.completedQuests(),
            newActiveQuests,
            state.actionHistory(),
            state.equipment(),
            state.inventory(),
            Instant.now()
        );
    }

    // Complete a quest
    public static RPGState.PlayerState completeQuest(RPGState.PlayerState state, String questId) {
        var newActiveQuests = new java.util.ArrayList<>(state.activeQuests());
        var newCompletedQuests = new java.util.ArrayList<>(state.completedQuests());
        newActiveQuests.remove(questId);
        if (!newCompletedQuests.contains(questId)) newCompletedQuests.add(questId);
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            state.currentLocationId(),
            state.health(),
            state.skills(),
            state.relationships(),
            newCompletedQuests,
            newActiveQuests,
            state.actionHistory(),
            state.equipment(),
            state.inventory(),
            Instant.now()
        );
    }

    // Add an action to history
    public static RPGState.PlayerState addAction(RPGState.PlayerState state, String actionType, String target, String outcome, String locationId) {
        var newHistory = new java.util.ArrayList<>(state.actionHistory());
        newHistory.add(new RPGState.ActionHistory(
            actionType,
            target,
            outcome,
            locationId,
            Instant.now()
        ));
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            state.currentLocationId(),
            state.health(),
            state.skills(),
            state.relationships(),
            state.completedQuests(),
            state.activeQuests(),
            newHistory,
            state.equipment(),
            state.inventory(),
            Instant.now()
        );
        }
    
    // --- EQUIPMENT MANAGEMENT (Simple D&D Basic System) ---
    
    /**
     * Equip an item from inventory to a specific slot
     */
    public static RPGState.PlayerState equipItem(RPGState.PlayerState state, String itemId, String slot) {
        if (!state.inventory().contains(itemId)) {
            // Item not in inventory, cannot equip
            return state;
        }
        
        var newEquipment = new java.util.HashMap<>(state.equipment());
        var newInventory = new java.util.ArrayList<>(state.inventory());
        
        // Remove item from inventory
        newInventory.remove(itemId);
        
        // If slot already occupied, move current item to inventory
        String currentItem = newEquipment.get(slot.toLowerCase());
        if (currentItem != null) {
            newInventory.add(currentItem);
        }
        
        // Equip new item
        newEquipment.put(slot.toLowerCase(), itemId);
        
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            state.currentLocationId(),
            state.health(),
            state.skills(),
            state.relationships(),
            state.completedQuests(),
            state.activeQuests(),
            state.actionHistory(),
            newEquipment,
            newInventory,
            Instant.now()
        );
    }
    
    /**
     * Unequip an item from a slot to inventory
     */
    public static RPGState.PlayerState unequipItem(RPGState.PlayerState state, String slot) {
        var newEquipment = new java.util.HashMap<>(state.equipment());
        var newInventory = new java.util.ArrayList<>(state.inventory());
        
        String item = newEquipment.remove(slot.toLowerCase());
        if (item != null) {
            newInventory.add(item);
        }
        
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            state.currentLocationId(),
            state.health(),
            state.skills(),
            state.relationships(),
            state.completedQuests(),
            state.activeQuests(),
            state.actionHistory(),
            newEquipment,
            newInventory,
            Instant.now()
        );
    }
    
    /**
     * Add item to inventory (from finding, buying, etc.)
     */
    public static RPGState.PlayerState addItemToInventory(RPGState.PlayerState state, String itemId) {
        var newInventory = new java.util.ArrayList<>(state.inventory());
        if (!newInventory.contains(itemId)) {
            newInventory.add(itemId);
        }
        
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            state.currentLocationId(),
            state.health(),
            state.skills(),
            state.relationships(),
            state.completedQuests(),
            state.activeQuests(),
            state.actionHistory(),
            state.equipment(),
            newInventory,
            Instant.now()
        );
    }
    
    /**
     * Remove item from inventory (dropped, sold, consumed, etc.)
     */
    public static RPGState.PlayerState removeItemFromInventory(RPGState.PlayerState state, String itemId) {
        var newInventory = new java.util.ArrayList<>(state.inventory());
        newInventory.remove(itemId);
        
        return new RPGState.PlayerState(
            state.playerId(),
            state.name(),
            state.currentLocationId(),
            state.health(),
            state.skills(),
            state.relationships(),
            state.completedQuests(),
            state.activeQuests(),
            state.actionHistory(),
            state.equipment(),
            newInventory,
            Instant.now()
        );
    }
    
    // --- SIMPLE COMBAT SYSTEM (D&D Basic Rules) ---
    
    /**
     * Resolve a simple combat attack
     */
    public static CombatResult resolveCombat(RPGState.PlayerState attacker, String targetName) {
        // Simple D&D Basic combat
        int attackRoll = (int)(Math.random() * 20) + 1; // 1d20
        int targetAC = 12; // Default target AC for monsters
        
        // Calculate attack bonus based on equipped weapon
        int attackBonus = 0;
        String weapon = attacker.getEquippedItem("weapon");
        if (weapon != null) {
            attackBonus = switch (weapon.toLowerCase()) {
                case "sword", "mace" -> 1;
                case "two_handed_sword" -> 2;
                default -> 0;
            };
        }
        
        int totalAttack = attackRoll + attackBonus;
        
        if (totalAttack >= targetAC) {
            // Hit! Roll damage
            String damageRoll = attacker.getWeaponDamage();
            int damage = rollDamage(damageRoll);
            return new CombatResult(true, damage, 
                String.format("Colpisci %s per %d danni! (Attacco: %d)", targetName, damage, totalAttack));
        } else {
            return new CombatResult(false, 0, 
                String.format("Il tuo attacco contro %s fallisce! (Attacco: %d vs AC %d)", targetName, totalAttack, targetAC));
        }
    }
    
    private static int rollDamage(String diceExpression) {
        // Simple dice rolling for damage
        return switch (diceExpression) {
            case "1d2" -> (int)(Math.random() * 2) + 1;
            case "1d4" -> (int)(Math.random() * 4) + 1;
            case "1d6" -> (int)(Math.random() * 6) + 1;
            case "1d8" -> (int)(Math.random() * 8) + 1;
            default -> 1;
        };
    }
    
    /**
     * Simple combat result
     */
    public record CombatResult(boolean hit, int damage, String description) {}
    
    // Add more KISS business logic as needed for NPCs, locations, etc.
}
