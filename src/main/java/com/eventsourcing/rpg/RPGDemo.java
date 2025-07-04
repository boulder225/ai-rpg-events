package com.eventsourcing.rpg;

import com.eventsourcing.core.infrastructure.InMemoryEventStore;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Demonstration of the AI-RPG event sourcing system.
 * Shows autonomous agents, persistent worlds, and evolving relationships.
 */
public class RPGDemo {
    
    public static void main(String[] args) {
        // Initialize event store and command handler
        var eventStore = new InMemoryEventStore<RPGEvent>();
        var commandHandler = new RPGCommandHandler(eventStore);
        
        System.out.println("=== AI-RPG Event Sourcing Demo ===");
        System.out.println();
        
        // Create a player
        var playerId = "player-001";
        var playerName = "Aria the Explorer";
        createPlayer(commandHandler, playerId, playerName);
        
        // Create an NPC
        var npcId = "npc-merchant-001";
        var npcName = "Gideon the Trader";
        createNPC(commandHandler, npcId, npcName);
        
        // Demonstrate persistent world interactions
        demonstratePlayerJourney(commandHandler, playerId);
        
        // Demonstrate evolving NPC
        demonstrateNPCEvolution(commandHandler, npcId);
        
        // Demonstrate social relationships
        demonstrateSocialNetwork(commandHandler, playerId, npcId);
        
        // Show current state
        showCurrentState(commandHandler, playerId, npcId);
        
        // Demonstrate time-travel queries
        demonstrateTimeTravel(commandHandler, playerId);
        
        System.out.println("Demo completed!");
    }
    
    private static void createPlayer(RPGCommandHandler handler, String playerId, String name) {
        System.out.println("Creating player: " + name);
        
        handler.executePlayerCommand(playerId, playerState -> 
            RPGBusinessLogic.createPlayer(new RPGCommand.CreatePlayer(
                UUID.randomUUID().toString(),
                playerId,
                name,
                Instant.now()
            ))
        );
        
        System.out.println("✓ Player created");
        System.out.println();
    }
    
    private static void createNPC(RPGCommandHandler handler, String npcId, String name) {
        System.out.println("Creating NPC: " + name);
        
        handler.executeNPCCommand(npcId, npcState -> 
            RPGBusinessLogic.createNPC(new RPGCommand.CreateNPC(
                UUID.randomUUID().toString(),
                npcId,
                name,
                "merchant",
                "town-square",
                Instant.now()
            ))
        );
        
        System.out.println("✓ NPC created");
        System.out.println();
    }
    
    private static void demonstratePlayerJourney(RPGCommandHandler handler, String playerId) {
        System.out.println("=== Player Journey Demo ===");
        
        // Player discovers a location
        handler.executeLocationCommand("forest-clearing", locationState -> 
            RPGBusinessLogic.discoverLocation(new RPGCommand.DiscoverLocation(
                UUID.randomUUID().toString(),
                playerId,
                "forest-clearing",
                "wilderness",
                Instant.now()
            ))
        );
        System.out.println("✓ Player discovered: Forest Clearing");
        
        // Player moves to the location
        handler.executePlayerCommand(playerId, playerState -> 
            RPGBusinessLogic.movePlayer(playerState, new RPGCommand.MovePlayer(
                UUID.randomUUID().toString(),
                playerId,
                "forest-clearing",
                Instant.now()
            ))
        );
        System.out.println("✓ Player moved to Forest Clearing");
        
        // Player performs an action
        handler.executePlayerCommand(playerId, playerState -> 
            RPGBusinessLogic.performAction(playerState, new RPGCommand.PerformAction(
                UUID.randomUUID().toString(),
                playerId,
                "explore",
                "ancient_ruins",
                Map.of("locationId", "forest-clearing"),
                Instant.now()
            ))
        );
        System.out.println("✓ Player explored ancient ruins");
        
        // Player starts a quest
        handler.executePlayerCommand(playerId, playerState -> 
            RPGBusinessLogic.startQuest(playerState, new RPGCommand.StartQuest(
                UUID.randomUUID().toString(),
                playerId,
                "find-lost-artifact",
                "mysterious-voice",
                Instant.now()
            ))
        );
        System.out.println("✓ Player started quest: Find Lost Artifact");
        System.out.println();
    }
    
    private static void demonstrateNPCEvolution(RPGCommandHandler handler, String npcId) {
        System.out.println("=== NPC Evolution Demo ===");
        
        // NPC learns a new skill autonomously
        handler.executeNPCCommand(npcId, npcState -> 
            RPGBusinessLogic.npcLearnSkill(npcState, new RPGCommand.NPCLearnSkill(
                UUID.randomUUID().toString(),
                npcId,
                "negotiation",
                "self-taught",
                Instant.now()
            ))
        );
        System.out.println("✓ NPC learned skill: Negotiation");
        
        // AI sets a new goal for the NPC
        handler.executeNPCCommand(npcId, npcState -> 
            RPGBusinessLogic.setNPCGoal(npcState, "expand_trade_routes", "market_demand_increased")
        );
        System.out.println("✓ AI set new NPC goal: Expand Trade Routes");
        
        // NPC moves to pursue their goal
        handler.executeNPCCommand(npcId, npcState -> 
            new com.eventsourcing.core.domain.CommandResult.Success<>(java.util.List.of(
                new RPGEvent.NPCMovedToLocation(
                    UUID.randomUUID().toString(),
                    npcId,
                    "town-square",
                    "trade-district",
                    "seeking_new_opportunities",
                    Instant.now()
                )
            ))
        );
        System.out.println("✓ NPC moved autonomously to Trade District");
        System.out.println();
    }
    
    private static void demonstrateSocialNetwork(RPGCommandHandler handler, String playerId, String npcId) {
        System.out.println("=== Social Network Demo ===");
        
        // Player initiates conversation with NPC
        handler.executePlayerCommand(playerId, playerState -> 
            RPGBusinessLogic.initiateConversation(playerState, new RPGCommand.InitiateConversation(
                UUID.randomUUID().toString(),
                playerId,
                npcId,
                "trade_opportunities",
                Instant.now()
            ))
        );
        System.out.println("✓ Player had conversation with NPC about trade");
        
        // Relationship evolves based on interaction
        handler.executePlayerCommand(playerId, playerState -> 
            new com.eventsourcing.core.domain.CommandResult.Success<>(java.util.List.of(
                new RPGEvent.RelationshipChanged(
                    UUID.randomUUID().toString(),
                    playerId,
                    npcId,
                    "acquaintance",
                    "business_partner",
                    15,
                    "successful_negotiation",
                    Instant.now()
                )
            ))
        );
        System.out.println("✓ Relationship evolved: Acquaintance → Business Partner");
        System.out.println();
    }
    
    private static void showCurrentState(RPGCommandHandler handler, String playerId, String npcId) {
        System.out.println("=== Current World State ===");
        
        var playerState = handler.getPlayerState(playerId);
        System.out.println("Player: " + playerState.name());
        System.out.println("  Location: " + playerState.currentLocationId());
        System.out.println("  Health: " + playerState.health());
        System.out.println("  Active Quests: " + playerState.activeQuests().size());
        System.out.println("  Relationships: " + playerState.relationships().size());
        System.out.println("  Actions Performed: " + playerState.actionHistory().size());
        
        var npcState = handler.getNPCState(npcId);
        System.out.println("\nNPC: " + npcState.name());
        System.out.println("  Type: " + npcState.type());
        System.out.println("  Location: " + npcState.currentLocationId());
        System.out.println("  Current Goal: " + npcState.currentGoal());
        System.out.println("  Skills: " + npcState.skills().size());
        System.out.println("  Known Players: " + npcState.knownPlayers().size());
        System.out.println();
    }
    
    private static void demonstrateTimeTravel(RPGCommandHandler handler, String playerId) {
        System.out.println("=== Time Travel Query Demo ===");
        
        var now = Instant.now();
        var pastState = handler.getPlayerStateAt(playerId, now.minusSeconds(60));
        
        System.out.println("Player state 1 minute ago:");
        System.out.println("  Actions performed: " + pastState.actionHistory().size());
        System.out.println("  Compare to current: " + handler.getPlayerState(playerId).actionHistory().size());
        System.out.println("✓ Time-travel query successful");
        System.out.println();
    }
}
