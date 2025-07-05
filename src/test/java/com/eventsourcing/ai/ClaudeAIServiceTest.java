package com.eventsourcing.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Claude AI integration.
 */
class ClaudeAIServiceTest {
    
    private ClaudeAIService aiService;
    
    @BeforeEach
    void setUp() {
        // Test with empty config (will use fallback responses)
        var config = new AIConfig("", "claude-3-sonnet-20240229", 1000, 0.7);
        aiService = new ClaudeAIService(config);
    }
    
    @Test
    void testFallbackGameMasterResponse() {
        var context = "Player is in a tavern";
        var action = "/look around";
        
        var response = aiService.generateGameMasterResponse(context, action);
        
        assertNotNull(response);
        assertFalse(response.fromAI()); // Should be fallback
        assertNotNull(response.content());
        assertTrue(response.content().contains("survey"));
    }
    
    @Test
    void testFallbackNPCDialogue() {
        var response = aiService.generateNPCDialogue(
            "Marcus", 
            "friendly tavern keeper", 
            "player greets NPC", 
            "Hello!"
        );
        
        assertNotNull(response);
        assertFalse(response.fromAI()); // Should be fallback
        assertNotNull(response.content());
        assertTrue(response.content().contains("Marcus"));
    }
    
    @Test
    void testFallbackWorldEvent() {
        var response = aiService.generateWorldEvent("storm", "peaceful village");
        
        assertNotNull(response);
        assertFalse(response.fromAI()); // Should be fallback
        assertNotNull(response.content());
        assertTrue(response.content().contains("storm"));
    }
    
    @Test
    void testAIMetrics() {
        var metrics = aiService.getMetrics();
        
        assertNotNull(metrics);
        assertEquals(0, metrics.getTotalRequests());
        assertEquals(0.0, metrics.getSuccessRate());
    }
    
    @Test
    void testIsConfigured() {
        assertFalse(aiService.isConfigured());
        
        // Test with valid config
        var configuredService = new ClaudeAIService(
            new AIConfig("test-key", "claude-3-sonnet-20240229", 1000, 0.7)
        );
        assertTrue(configuredService.isConfigured());
    }
}
