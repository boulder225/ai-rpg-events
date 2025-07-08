package com.eventsourcing.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the AI-RPG API Server.
 * Tests the complete event sourcing + REST API integration.
 */
class RPGApiServerTest {
    
    private RPGApiServer server;
    private HttpClient client;
    private static final int TEST_PORT = 8888;
    
    @BeforeEach
    void setUp() throws IOException {
        server = new RPGApiServer(TEST_PORT);
        server.start();
        client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        
        // Give server time to start
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }
    
    @Test
    void testCreateSessionEndpoint() throws Exception {
        var requestBody = """
            {
                "player_id": "test_player_123",
                "player_name": "Test Hero"
            }
            """;
        
        var request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + TEST_PORT + "/api/session/create"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        System.out.println("RESPONSE BODY: " + response.body());
        assertTrue(response.body().contains("\"success\":true"));
        assertTrue(response.body().contains("session_id"));
    }
    
    @Test
    void testWebInterfaceEndpoint() throws Exception {
        var request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + TEST_PORT + "/"))
            .GET()
            .build();
        
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("AI-RPG Event Sourcing Platform"));
        assertTrue(response.body().contains("html"));
    }
    
    @Test
    void testMetricsEndpoint() throws Exception {
        var request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + TEST_PORT + "/api/metrics"))
            .GET()
            .build();
        
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("success"));
        assertTrue(response.body().contains("total_sessions"));
        assertTrue(response.body().contains("total_actions"));
    }
}
