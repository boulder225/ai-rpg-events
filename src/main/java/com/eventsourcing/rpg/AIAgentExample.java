package com.eventsourcing.rpg;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Example of how AI agents would integrate with the event sourcing system.
 * Shows autonomous decision-making based on complete world context.
 */
public class AIAgentExample {
    
    private final RPGCommandHandler commandHandler;
    private final Random random = new Random();
    
    public AIAgentExample(RPGCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }
    
    /**
     * AI Game Master making autonomous decisions based on player context.
     */
    public void autonomousGameMaster(String playerId) {
        var playerState = commandHandler.getPlayerState(playerId);
        
        // AI analyzes complete player context
        var recentActions = playerState.actionHistory().stream()
            .filter(action -> action.timestamp().isAfter(Instant.now().minusSeconds(300)))
            .toList();
        
        // AI decision: If player has been exploring a lot, spawn a discovery event
        if (recentActions.stream().anyMatch(action -> "explore".equals(action.actionType()))) {
            triggerDiscoveryEvent(playerId, playerState.currentLocationId());
        }
        
        // AI decision: If player has high trust with NPCs, create collaborative opportunities
        var trustedNPCs = playerState.relationships().entrySet().stream()
            .filter(entry -> entry.getValue().trustLevel() > 70)
            .map(entry -> entry.getKey())
            .toList();
        
        if (!trustedNPCs.isEmpty()) {
            createCollaborativeQuest(playerId, trustedNPCs.get(0));
        }
    }
    
    /**
     * Autonomous NPC behavior based on goals and world state.
     */
    public void autonomousNPCBehavior(String npcId) {
        var npcState = commandHandler.getNPCState(npcId);
        
        // AI decision: NPC pursues their current goal
        switch (npcState.currentGoal()) {
            case "expand_trade_routes" -> {
                // NPC autonomously moves to seek trade opportunities
                if ("town-square".equals(npcState.currentLocationId())) {
                    moveNPCToLocation(npcId, "trade-district", "seeking_customers");
                }
                
                // NPC learns skills relevant to their goal
                if (!npcState.skills().containsKey("negotiation")) {
                    learnSkill(npcId, "negotiation");
                }
            }
            case "seek_knowledge" -> {
                // NPC moves to library or seeks conversations with learned players
                if (npcState.knownPlayers().isEmpty()) {
                    moveNPCToLocation(npcId, "town-center", "meeting_people");
                }
            }
        }
        
        // AI decision: Evolve NPC goals based on world events and interactions
        if (npcState.skills().size() > 3 && "expand_trade_routes".equals(npcState.currentGoal())) {
            // NPC has learned enough, time for a new goal
            setNewNPCGoal(npcId, "establish_guild", "accumulated_sufficient_expertise");
        }
    }
    
    /**
     * World event system triggering autonomous changes.
     */
    public void autonomousWorldEvents() {
        // AI triggers random world events that affect multiple entities
        var eventTypes = List.of("market_fluctuation", "weather_change", "festival_announcement", "monster_sighting");
        var selectedEvent = eventTypes.get(random.nextInt(eventTypes.size()));
        
        switch (selectedEvent) {
            case "market_fluctuation" -> {
                // Affects all merchant NPCs
                commandHandler.executeLocationCommand("trade-district", locationState -> 
                    RPGBusinessLogic.triggerWorldEvent(
                        "market_fluctuation",
                        "Prices shift dramatically in the trade district",
                        locationState.currentOccupants()
                    )
                );
            }
            case "festival_announcement" -> {
                // Creates new opportunities for social interaction
                commandHandler.executeLocationCommand("town-center", locationState -> 
                    RPGBusinessLogic.triggerWorldEvent(
                        "festival_announcement",
                        "Harvest festival announced for next week",
                        locationState.currentOccupants()
                    )
                );
            }
        }
    }
    
    /**
     * AI analyzes player patterns to predict needs and create proactive content.
     */
    public void predictiveContentGeneration(String playerId) {
        var playerState = commandHandler.getPlayerState(playerId);
        
        // Analyze player's preferred activities
        var actionTypes = playerState.actionHistory().stream()
            .map(RPGState.ActionHistory::actionType)
            .toList();
        
        var explorationCount = actionTypes.stream().mapToInt(type -> "explore".equals(type) ? 1 : 0).sum();
        var combatCount = actionTypes.stream().mapToInt(type -> "attack".equals(type) ? 1 : 0).sum();
        var socialCount = actionTypes.stream().mapToInt(type -> "persuade".equals(type) ? 1 : 0).sum();
        
        // AI creates content matching player preferences
        if (explorationCount > combatCount && explorationCount > socialCount) {
            // Player prefers exploration - generate mysterious locations
            generateExplorationContent(playerId);
        } else if (combatCount > socialCount) {
            // Player prefers combat - spawn challenging encounters
            generateCombatContent(playerId);
        } else {
            // Player prefers social interaction - create complex NPCs
            generateSocialContent(playerId);
        }
    }
    
    private void triggerDiscoveryEvent(String playerId, String locationId) {
        commandHandler.executePlayerCommand(playerId, playerState -> 
            new com.eventsourcing.core.domain.CommandResult.Success<>(List.of(
                new RPGEvent.ItemDiscovered(
                    UUID.randomUUID().toString(),
                    playerId,
                    "ancient-artifact-" + random.nextInt(1000),
                    "artifact",
                    locationId,
                    Instant.now()
                )
            ))
        );
    }
    
    private void createCollaborativeQuest(String playerId, String npcId) {
        commandHandler.executePlayerCommand(playerId, playerState -> 
            RPGBusinessLogic.startQuest(playerState, new RPGCommand.StartQuest(
                UUID.randomUUID().toString(),
                playerId,
                "collaborative-venture-" + random.nextInt(1000),
                npcId,
                Instant.now()
            ))
        );
    }
    
    private void moveNPCToLocation(String npcId, String locationId, String purpose) {
        commandHandler.executeNPCCommand(npcId, npcState -> 
            new com.eventsourcing.core.domain.CommandResult.Success<>(List.of(
                new RPGEvent.NPCMovedToLocation(
                    UUID.randomUUID().toString(),
                    npcId,
                    npcState.currentLocationId(),
                    locationId,
                    purpose,
                    Instant.now()
                )
            ))
        );
    }
    
    private void learnSkill(String npcId, String skillName) {
        commandHandler.executeNPCCommand(npcId, npcState -> 
            RPGBusinessLogic.npcLearnSkill(npcState, new RPGCommand.NPCLearnSkill(
                UUID.randomUUID().toString(),
                npcId,
                skillName,
                "self-directed-learning",
                Instant.now()
            ))
        );
    }
    
    private void setNewNPCGoal(String npcId, String newGoal, String reason) {
        commandHandler.executeNPCCommand(npcId, npcState -> 
            RPGBusinessLogic.setNPCGoal(npcState, newGoal, reason)
        );
    }
    
    private void generateExplorationContent(String playerId) {
        // AI generates new mysterious locations based on player preferences
        var newLocationId = "mystery-location-" + random.nextInt(1000);
        commandHandler.executeLocationCommand(newLocationId, locationState -> 
            RPGBusinessLogic.discoverLocation(new RPGCommand.DiscoverLocation(
                UUID.randomUUID().toString(),
                playerId,
                newLocationId,
                "mysterious_ruins",
                Instant.now()
            ))
        );
    }
    
    private void generateCombatContent(String playerId) {
        // AI spawns challenging encounters
        commandHandler.executePlayerCommand(playerId, playerState -> 
            new com.eventsourcing.core.domain.CommandResult.Success<>(List.of(
                new RPGEvent.ActionPerformed(
                    UUID.randomUUID().toString(),
                    playerId,
                    "encounter",
                    "challenging_monster",
                    "combat_initiated",
                    java.util.Map.of("difficulty", "high", "type", "boss"),
                    Instant.now()
                )
            ))
        );
    }
    
    private void generateSocialContent(String playerId) {
        // AI creates complex NPCs with rich backstories
        var newNPCId = "complex-npc-" + random.nextInt(1000);
        commandHandler.executeNPCCommand(newNPCId, npcState -> 
            RPGBusinessLogic.createNPC(new RPGCommand.CreateNPC(
                UUID.randomUUID().toString(),
                newNPCId,
                "Mysterious Stranger",
                "complex_character",
                "tavern",
                Instant.now()
            ))
        );
    }
}
