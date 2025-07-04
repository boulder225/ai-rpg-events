package com.eventsourcing.rpg;

import com.eventsourcing.core.infrastructure.InMemoryEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the AI-RPG event sourcing system.
 * Validates autonomous agent behavior and persistent world state.
 */
class RPGSystemTest {
    
    private InMemoryEventStore<RPGEvent> eventStore;
    private RPGCommandHandler commandHandler;
    
    @BeforeEach
    void setUp() {
        eventStore = new InMemoryEventStore<>();
        commandHandler = new RPGCommandHandler(eventStore);
    }
    
    @Test
    void testPlayerCreationAndState() {
        // Given
        var playerId = "test-player";
        var playerName = "Test Hero";
        
        // When
        commandHandler.executePlayerCommand(playerId, playerState -> 
            RPGBusinessLogic.createPlayer(new RPGCommand.CreatePlayer(
                UUID.randomUUID().toString(),
                playerId,
                playerName,
                Instant.now()
            ))
        );
        
        // Then
        var playerState = commandHandler.getPlayerState(playerId);
        assertEquals(playerId, playerState.playerId());
        assertEquals(playerName, playerState.name());
        assertEquals(100, playerState.health());
        assertTrue(playerState.activeQuests().isEmpty());
        assertTrue(playerState.relationships().isEmpty());
    }
    
    @Test
    void testPlayerMovementAndLocationTracking() {
        // Given
        var playerId = "test-player";
        createTestPlayer(playerId);
        
        // When - Player moves to new location
        commandHandler.executePlayerCommand(playerId, playerState -> 
            RPGBusinessLogic.movePlayer(playerState, new RPGCommand.MovePlayer(
                UUID.randomUUID().toString(),
                playerId,
                "forest-clearing",
                Instant.now()
            ))
        );
        
        // Then
        var playerState = commandHandler.getPlayerState(playerId);
        assertEquals("forest-clearing", playerState.currentLocationId());
    }
    
    @Test
    void testNPCEvolutionAndSkillLearning() {
        // Given
        var npcId = "test-npc";
        createTestNPC(npcId);
        
        // When - NPC learns a skill
        commandHandler.executeNPCCommand(npcId, npcState -> 
            RPGBusinessLogic.npcLearnSkill(npcState, new RPGCommand.NPCLearnSkill(
                UUID.randomUUID().toString(),
                npcId,
                "combat",
                "training",
                Instant.now()
            ))
        );
        
        // Then
        var npcState = commandHandler.getNPCState(npcId);
        assertTrue(npcState.skills().containsKey("combat"));
        assertEquals(1, npcState.skills().get("combat"));
    }
    
    @Test
    void testSocialRelationshipFormation() {
        // Given
        var playerId = "test-player";
        var npcId = "test-npc";
        createTestPlayer(playerId);
        createTestNPC(npcId);
        
        // When - Player initiates conversation
        commandHandler.executePlayerCommand(playerId, playerState -> 
            RPGBusinessLogic.initiateConversation(playerState, new RPGCommand.InitiateConversation(
                UUID.randomUUID().toString(),
                playerId,
                npcId,
                "greeting",
                Instant.now()
            ))
        );
        
        // Then
        var playerState = commandHandler.getPlayerState(playerId);
        assertTrue(playerState.relationships().containsKey(npcId));
        assertEquals("acquaintance", playerState.relationships().get(npcId).relationType());
    }
    
    @Test
    void testQuestManagement() {
        // Given
        var playerId = "test-player";
        createTestPlayer(playerId);
        
        // When - Player starts a quest
        commandHandler.executePlayerCommand(playerId, playerState -> 
            RPGBusinessLogic.startQuest(playerState, new RPGCommand.StartQuest(
                UUID.randomUUID().toString(),
                playerId,
                "test-quest",
                "quest-giver",
                Instant.now()
            ))
        );
        
        // Then
        var playerState = commandHandler.getPlayerState(playerId);
        assertTrue(playerState.activeQuests().contains("test-quest"));
        assertFalse(playerState.completedQuests().contains("test-quest"));
    }
    
    @Test
    void testActionHistoryTracking() {
        // Given
        var playerId = "test-player";
        createTestPlayer(playerId);
        
        // When - Player performs an action
        commandHandler.executePlayerCommand(playerId, playerState -> 
            RPGBusinessLogic.performAction(playerState, new RPGCommand.PerformAction(
                UUID.randomUUID().toString(),
                playerId,
                "explore",
                "cave",
                Map.of("locationId", "forest"),
                Instant.now()
            ))
        );
        
        // Then
        var playerState = commandHandler.getPlayerState(playerId);
        assertEquals(1, playerState.actionHistory().size());
        assertEquals("explore", playerState.actionHistory().get(0).actionType());
        assertEquals("cave", playerState.actionHistory().get(0).target());
    }
    
    @Test
    void testTimeBasedQueries() {
        // Given
        var playerId = "test-player";
        createTestPlayer(playerId);
        var timestamp1 = Instant.now();
        
        // Perform first action
        commandHandler.executePlayerCommand(playerId, playerState -> 
            RPGBusinessLogic.performAction(playerState, new RPGCommand.PerformAction(
                UUID.randomUUID().toString(),
                playerId,
                "explore",
                "cave",
                Map.of(),
                timestamp1
            ))
        );
        
        var timestamp2 = timestamp1.plusSeconds(10);
        
        // Perform second action
        commandHandler.executePlayerCommand(playerId, playerState -> 
            RPGBusinessLogic.performAction(playerState, new RPGCommand.PerformAction(
                UUID.randomUUID().toString(),
                playerId,
                "fight",
                "monster",
                Map.of(),
                timestamp2
            ))
        );
        
        // When - Query state at different times
        var stateAtTime1 = commandHandler.getPlayerStateAt(playerId, timestamp1.plusSeconds(1));
        var currentState = commandHandler.getPlayerState(playerId);
        
        // Then
        assertEquals(1, stateAtTime1.actionHistory().size());
        assertEquals(2, currentState.actionHistory().size());
    }
    
    @Test
    void testConcurrentModificationHandling() {
        // Given
        var playerId = "test-player";
        createTestPlayer(playerId);
        
        // When - Simulate concurrent modifications
        assertDoesNotThrow(() -> {
            commandHandler.executePlayerCommand(playerId, playerState -> 
                RPGBusinessLogic.performAction(playerState, new RPGCommand.PerformAction(
                    UUID.randomUUID().toString(),
                    playerId,
                    "action1",
                    "target1",
                    Map.of(),
                    Instant.now()
                ))
            );
            
            commandHandler.executePlayerCommand(playerId, playerState -> 
                RPGBusinessLogic.performAction(playerState, new RPGCommand.PerformAction(
                    UUID.randomUUID().toString(),
                    playerId,
                    "action2",
                    "target2",
                    Map.of(),
                    Instant.now()
                ))
            );
        });
        
        // Then
        var playerState = commandHandler.getPlayerState(playerId);
        assertEquals(2, playerState.actionHistory().size());
    }
    
    private void createTestPlayer(String playerId) {
        commandHandler.executePlayerCommand(playerId, playerState -> 
            RPGBusinessLogic.createPlayer(new RPGCommand.CreatePlayer(
                UUID.randomUUID().toString(),
                playerId,
                "Test Player",
                Instant.now()
            ))
        );
    }
    
    private void createTestNPC(String npcId) {
        commandHandler.executeNPCCommand(npcId, npcState -> 
            RPGBusinessLogic.createNPC(new RPGCommand.CreateNPC(
                UUID.randomUUID().toString(),
                npcId,
                "Test NPC",
                "merchant",
                "town",
                Instant.now()
            ))
        );
    }
}
