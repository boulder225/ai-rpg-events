package com.eventsourcing.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Claude AI integration with sealed interface AIResponse.
 */
class ClaudeAIServiceTest {
    
    private ClaudeAIService aiService;
    
    @BeforeEach
    void setUp() {
        // Test with empty config (will use fallback responses)
        var config = AIConfig.forTesting("", "claude-3-sonnet-20240229", 1000, 0.7);
        aiService = new ClaudeAIService(config);
    }
    
    @Test
    void testFallbackGameMasterResponse() {
        var context = "Player is in a tavern";
        var action = "/look around";
        
        var response = aiService.generateGameMasterResponse(context, action);
        
        assertNotNull(response);
        assertFalse(response.isSuccess()); // Should be fallback
        assertNotNull(response.content());
        assertTrue(response.content().contains("Osservi") || response.content().contains("dintorni"));
        
        // Pattern matching verification
        switch (response) {
            case AIResponse.Success success -> fail("Should not be success");
            case AIResponse.Fallback fallback -> {
                assertNotNull(fallback.reason());
                assertTrue(fallback.reason().contains("not configured"));
            }
            case AIResponse.Error error -> fail("Should not be error");
        }
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
        assertFalse(response.isSuccess()); // Should be fallback
        assertNotNull(response.content());
        assertTrue(response.content().contains("Marcus"));
        
        // Verify it's a fallback response
        switch (response) {
            case AIResponse.Fallback fallback -> {
                assertNotNull(fallback.reason());
                assertEquals("AI service not available", fallback.reason());
            }
            case AIResponse.Success success -> fail("Should not be success");
            case AIResponse.Error error -> fail("Should not be error");
        }
    }
    
    @Test
    void testFallbackWorldEvent() {
        var response = aiService.generateWorldEvent("storm", "peaceful village");
        
        assertNotNull(response);
        assertFalse(response.isSuccess()); // Should be fallback
        assertNotNull(response.content());
        assertTrue(response.content().contains("storm"));
        
        // Verify it's a fallback response
        switch (response) {
            case AIResponse.Fallback fallback -> {
                assertNotNull(fallback.reason());
                assertEquals("AI service not available", fallback.reason());
            }
            case AIResponse.Success success -> fail("Should not be success");
            case AIResponse.Error error -> fail("Should not be error");
        }
    }
    
    @Test
    void testFactoryMethods() {
        // Test success factory method
        var successResponse = AIResponse.success("Hello world", 10, 5);
        assertTrue(successResponse.isSuccess());
        assertEquals("Hello world", successResponse.content());
        
        switch (successResponse) {
            case AIResponse.Success success -> {
                assertEquals(10, success.inputTokens());
                assertEquals(5, success.outputTokens());
                assertEquals(15, success.getTotalTokens());
            }
            case AIResponse.Fallback fallback -> fail("Should be success");
            case AIResponse.Error error -> fail("Should be success");
        }
        
        // Test fallback factory method
        var fallbackResponse = AIResponse.fallback("Fallback content", "Rate limited");
        assertFalse(fallbackResponse.isSuccess());
        assertEquals("Fallback content", fallbackResponse.content());
        
        switch (fallbackResponse) {
            case AIResponse.Fallback fallback -> {
                assertEquals("Rate limited", fallback.reason());
            }
            case AIResponse.Success success -> fail("Should be fallback");
            case AIResponse.Error error -> fail("Should be fallback");
        }
        
        // Test error factory method
        var errorResponse = AIResponse.error("Network error");
        assertFalse(errorResponse.isSuccess());
        assertEquals("", errorResponse.content()); // Error responses have empty content
        
        switch (errorResponse) {
            case AIResponse.Error error -> {
                assertEquals("Network error", error.errorMessage());
            }
            case AIResponse.Success success -> fail("Should be error");
            case AIResponse.Fallback fallback -> fail("Should be error");
        }
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
            AIConfig.forTesting("test-key", "claude-3-sonnet-20240229", 1000, 0.7)
        );
        assertTrue(configuredService.isConfigured());
    }
    
    @Test
    void testPatternMatchingExhaustiveness() {
        // Test that all AIResponse types are handled
        var responses = new AIResponse[] {
            AIResponse.success("Test content", 5, 3),
            AIResponse.fallback("Fallback content", "Rate limited"),
            AIResponse.error("Test error")
        };
        
        for (var response : responses) {
            // This should compile without warnings about exhaustiveness
            var result = switch (response) {
                case AIResponse.Success success -> "success";
                case AIResponse.Fallback fallback -> "fallback";
                case AIResponse.Error error -> "error";
            };
            assertNotNull(result);
        }
    }
}
