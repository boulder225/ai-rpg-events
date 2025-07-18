package com.eventsourcing.rpg;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Read models for querying the current state of the RPG world.
 * These are rebuilt from events and represent the current "living" state.
 */
public class RPGState {
    
    /**
     * Complete player context including narrative history and current state.
     * ENHANCED: Added simple equipment and inventory system.
     */
    public record PlayerState(
        String playerId,
        String name,
        String currentLocationId,
        int health,
        Map<String, Integer> skills,
        Map<String, Relationship> relationships,
        List<String> completedQuests,
        List<String> activeQuests,
        List<ActionHistory> actionHistory,
        Map<String, String> equipment,  // slot -> itemId (weapon, armor, shield, etc.)
        List<String> inventory,          // items not currently equipped
        Instant lastSeen
    ) {
        
        /**
         * Get equipped item in a specific slot
         */
        public String getEquippedItem(String slot) {
            return equipment.get(slot.toLowerCase());
        }
        
        /**
         * Check if player has an item (equipped or in inventory)
         */
        public boolean hasItem(String itemId) {
            return equipment.containsValue(itemId) || inventory.contains(itemId);
        }
        
        /**
         * Get all items (equipped + inventory)
         */
        public List<String> getAllItems() {
            var allItems = new java.util.ArrayList<>(inventory);
            allItems.addAll(equipment.values());
            return allItems;
        }
        
        /**
         * Get armor class based on equipped items (D&D Basic rules)
         */
        public int getArmorClass() {
            String armor = getEquippedItem("armor");
            String shield = getEquippedItem("shield");
            
            int baseAC = 9; // Unarmored AC in D&D Basic
            
            // Simple armor calculation
            if (armor != null) {
                baseAC = switch (armor.toLowerCase()) {
                    case "leather_armor" -> 7;
                    case "chain_mail" -> 5;
                    case "plate_mail" -> 3;
                    default -> 8; // Generic light armor
                };
            }
            
            // Shield bonus
            if (shield != null) {
                baseAC -= 1; // Shield improves AC by 1
            }
            
            return Math.max(baseAC, 0); // AC can't go below 0
        }
        
        /**
         * Get current weapon damage dice (D&D Basic rules)
         */
        public String getWeaponDamage() {
            String weapon = getEquippedItem("weapon");
            if (weapon == null) return "1d2"; // Unarmed
            
            return switch (weapon.toLowerCase()) {
                case "dagger" -> "1d4";
                case "sword", "mace" -> "1d6";
                case "two_handed_sword" -> "1d8";
                case "spear" -> "1d6";
                default -> "1d4"; // Generic small weapon
            };
        }
    }
    
    /**
     * NPC state with autonomous evolution tracking.
     */
    public record NPCState(
        String npcId,
        String name,
        String type,
        String currentLocationId,
        Map<String, Integer> skills,
        String currentGoal,
        Map<String, Relationship> relationships,
        List<String> knownPlayers,
        Instant lastActivity
    ) {}
    
    /**
     * Location state with persistent memory of player interactions.
     */
    public record LocationState(
        String locationId,
        String type,
        Map<String, String> properties,
        List<String> visitedBy,
        Map<String, Integer> timeSpentBy,
        List<String> itemsFound,
        List<String> currentOccupants,
        Instant lastModified
    ) {}
    
    /**
     * Relationship tracking between entities.
     */
    public record Relationship(
        String entityId,
        String relationType,
        int trustLevel,
        int friendshipLevel,
        List<String> sharedExperiences,
        Instant lastInteraction
    ) {}
    
    /**
     * Action history for narrative context.
     */
    public record ActionHistory(
        String actionType,
        String target,
        String outcome,
        String locationId,
        Instant timestamp
    ) {}
    
    /**
     * World event tracking for autonomous agent context.
     */
    public record WorldEvent(
        String eventType,
        String description,
        List<String> affectedEntities,
        Instant occurredAt
    ) {}
}
