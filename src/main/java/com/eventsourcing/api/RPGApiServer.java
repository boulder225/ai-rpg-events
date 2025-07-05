package com.eventsourcing.api;

import com.eventsourcing.rpg.*;
import com.eventsourcing.core.infrastructure.InMemoryEventStore;
import com.eventsourcing.ai.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.eventsourcing.api.ApiModels.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Complete RESTful API server for the AI-RPG platform with Claude AI integration.
 */
public class RPGApiServer {
    
    private final HttpServer server;
    private final RPGCommandHandler commandHandler;
    private final ClaudeAIService aiService;
    private final ObjectMapper objectMapper;
    private final Map<String, String> activeSessions;
    private final RPGMetrics metrics;
    
    public RPGApiServer(int port) throws IOException {
        // Load environment variables
        EnvLoader.loadDotEnv();
        
        var eventStore = new InMemoryEventStore<RPGEvent>();
        this.commandHandler = new RPGCommandHandler(eventStore);
        
        // Initialize Claude AI service
        var aiConfig = new AIConfig();
        this.aiService = new ClaudeAIService(aiConfig);
        
        this.objectMapper = new ObjectMapper();
        this.activeSessions = new HashMap<>();
        this.metrics = new RPGMetrics();
        
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        setupRoutes();
        server.setExecutor(Executors.newFixedThreadPool(10));
        
        // Log AI configuration status
        if (aiService.isConfigured()) {
            System.out.println("🤖 Claude AI integration: ENABLED");
            System.out.println("💫 AI Model: " + aiConfig.getClaudeModel());
            if (aiConfig.getClaudeModel().contains("claude-4") || aiConfig.getClaudeModel().contains("sonnet-4") || aiConfig.getClaudeModel().contains("opus-4")) {
                System.out.println("🎆 CLAUDE 4 DETECTED - Latest AI power activated!");
            }
        } else {
            System.out.println("⚠️ Claude AI integration: DISABLED (using fallback responses)");
            System.out.println("📝 Set CLAUDE_API_KEY in .env file to enable Claude 4 AI responses");
        }
    }
    
    private void setupRoutes() {
        server.createContext("/api/session/create", new CreateSessionHandler());
        server.createContext("/api/game/action", new GameActionHandler());
        server.createContext("/api/game/status", new GameStatusHandler());
        server.createContext("/api/ai/prompt", new AIPromptHandler());
        server.createContext("/api/metrics", new MetricsHandler());
        server.createContext("/", new WebInterfaceHandler());
    }
    
    public void start() {
        server.start();
        System.out.println("🚀 AI-RPG Event Sourcing Server started!");
        System.out.println("🌐 URL: http://localhost:" + server.getAddress().getPort());
        System.out.println();
        System.out.println("📚 API Endpoints:");
        System.out.println("  POST /api/session/create - Create new adventure session");
        System.out.println("  POST /api/game/action - Execute game actions with AI responses");
        System.out.println("  GET  /api/game/status?session_id=X - Get complete world state");
        System.out.println("  GET  /api/ai/prompt?session_id=X - View AI context prompt");
        System.out.println("  GET  /api/metrics - System performance metrics");
        System.out.println("  GET  / - Interactive web interface");
        System.out.println();
        if (aiService.isConfigured()) {
            System.out.println("🎮 Ready for intelligent AI adventures with Claude!");
        } else {
            System.out.println("🎮 Ready for adventures with simulated AI responses!");
        }
    }
    
    public void stop() {
        server.stop(0);
        System.out.println("🛑 Server stopped");
    }
    
    // Session Creation Handler
    private class CreateSessionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            
            try {
                var request = parseRequest(exchange, SessionCreateRequest.class);
                
                if (request.playerId == null || request.playerName == null) {
                    sendErrorResponse(exchange, "PlayerID and PlayerName are required", 400);
                    return;
                }
                
                var sessionId = UUID.randomUUID().toString();
                activeSessions.put(sessionId, request.playerId);
                
                // Create player in event store
                commandHandler.executePlayerCommand(request.playerId, playerState -> 
                    RPGBusinessLogic.createPlayer(new RPGCommand.CreatePlayer(
                        UUID.randomUUID().toString(),
                        request.playerId,
                        request.playerName,
                        Instant.now()
                    ))
                );
                
                metrics.incrementSessions();
                
                // Generate welcome message with AI
                var context = String.format("New player %s has entered the world", request.playerName);
                var aiResponse = aiService.generateGameMasterResponse(context, "player joins adventure");
                
                var welcomeMessage = aiResponse.isSuccess() ? 
                    aiResponse.content() : 
                    String.format("🌟 Benvenuto, %s! La tua avventura inizia in un villaggio mistico dove la magia antica scorre attraverso strade di ciottoli.", request.playerName);
                
                var response = new GameResponse(
                    true,
                    welcomeMessage,
                    sessionId,
                    Map.of("ai_powered", aiResponse.isSuccess()),
                    null
                );
                
                sendJsonResponse(exchange, response, 200);
                
            } catch (Exception e) {
                sendErrorResponse(exchange, "Failed to create session: " + e.getMessage(), 500);
            }
        }
    }
    
    // Game Action Handler with Claude AI
    private class GameActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            
            try {
                var request = parseRequest(exchange, GameActionRequest.class);
                
                if (request.sessionId == null || request.command == null) {
                    sendErrorResponse(exchange, "SessionID and Command are required", 400);
                    return;
                }
                
                var playerId = activeSessions.get(request.sessionId);
                if (playerId == null) {
                    sendErrorResponse(exchange, "Session not found", 404);
                    return;
                }
                
                var result = processGameActionWithAI(playerId, request.command);
                metrics.incrementActions();
                
                sendJsonResponse(exchange, result, 200);
                
            } catch (Exception e) {
                sendErrorResponse(exchange, "Failed to process action: " + e.getMessage(), 500);
            }
        }
    }
    
    // Game Status Handler
    private class GameStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            
            try {
                var sessionId = getQueryParam(exchange, "session_id");
                if (sessionId == null) {
                    sendErrorResponse(exchange, "session_id parameter is required", 400);
                    return;
                }
                
                var playerId = activeSessions.get(sessionId);
                if (playerId == null) {
                    sendErrorResponse(exchange, "Session not found", 404);
                    return;
                }
                
                var playerState = commandHandler.getPlayerState(playerId);
                var context = buildContextSummary(playerState);
                
                var response = new GameResponse(
                    true,
                    "World state retrieved from persistent event streams",
                    sessionId,
                    context,
                    null
                );
                
                sendJsonResponse(exchange, response, 200);
                
            } catch (Exception e) {
                sendErrorResponse(exchange, "Failed to get status: " + e.getMessage(), 500);
            }
        }
    }
    
    // AI Prompt Handler
    private class AIPromptHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            
            try {
                var sessionId = getQueryParam(exchange, "session_id");
                if (sessionId == null) {
                    sendErrorResponse(exchange, "session_id parameter is required", 400);
                    return;
                }
                
                var playerId = activeSessions.get(sessionId);
                if (playerId == null) {
                    sendErrorResponse(exchange, "Session not found", 404);
                    return;
                }
                
                var prompt = generateAIPrompt(playerId);
                
                var response = new GameResponse(
                    true,
                    prompt,
                    null,
                    Map.of("ai_configured", aiService.isConfigured()),
                    null
                );
                
                sendJsonResponse(exchange, response, 200);
                
            } catch (Exception e) {
                sendErrorResponse(exchange, "Failed to generate prompt: " + e.getMessage(), 500);
            }
        }
    }
    
    // Metrics Handler with AI metrics
    private class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            
            var aiMetrics = aiService.getMetrics();
            var metricsData = Map.of(
                "context", Map.of(
                    "active_sessions", activeSessions.size(),
                    "total_actions_processed", metrics.getTotalActions(),
                    "total_sessions_created", metrics.getTotalSessions(),
                    "average_response_time_ms", metrics.getAverageResponseTime()
                ),
                "ai", Map.of(
                    "provider", aiService.isConfigured() ? "claude" : "fallback",
                    "configured", aiService.isConfigured(),
                    "total_requests", aiMetrics.getTotalRequests(),
                    "successful_requests", aiMetrics.getSuccessfulRequests(),
                    "failed_requests", aiMetrics.getFailedRequests(),
                    "cache_hits", aiMetrics.getCacheHits(),
                    "success_rate", String.format("%.2f%%", aiMetrics.getSuccessRate() * 100),
                    "cache_hit_rate", String.format("%.2f%%", aiMetrics.getCacheHitRate() * 100),
                    "total_tokens", aiMetrics.getTotalTokens()
                ),
                "server", Map.of(
                    "uptime", metrics.getUptime(),
                    "ai_provider", aiService.isConfigured() ? "claude-ai" : "simulation"
                )
            );
            
            var response = new GameResponse(
                true,
                "System metrics retrieved",
                null,
                metricsData,
                null
            );
            
            sendJsonResponse(exchange, response, 200);
        }
    }
    
    private class WebInterfaceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            var html = WebInterfaceGenerator.generateHTML();
            var bytes = html.getBytes(StandardCharsets.UTF_8);
            
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            
            try (var os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
    
    // Core game processing with Claude AI
    private GameResponse processGameActionWithAI(String playerId, String command) {
        var startTime = System.currentTimeMillis();
        
        try {
            var playerState = commandHandler.getPlayerState(playerId);
            
            // Generate full context for AI
            var gameContext = generateGameContextForAI(playerState);
            
            // Get AI response
            var aiResponse = aiService.generateGameMasterResponse(gameContext, command);
            
            // Record AI token usage if available
            if (aiResponse.isSuccess()) {
                aiService.getMetrics().recordTokenUsage(aiResponse.getTotalTokens());
            }
            
            // Process command and record events
            commandHandler.executePlayerCommand(playerId, state -> {
                return processCommandBasedOnType(playerId, command, state);
            });
            
            var updatedState = commandHandler.getPlayerState(playerId);
            var context = buildGameContext(updatedState, aiResponse);
            
            metrics.recordResponseTime(System.currentTimeMillis() - startTime);
            
            return new GameResponse(
                true, 
                aiResponse.content(), 
                null, 
                context, 
                null
            );
            
        } catch (Exception e) {
            return new GameResponse(
                false, 
                null, 
                null, 
                null, 
                "Failed to process action: " + e.getMessage()
            );
        }
    }
    
    private String generateGameContextForAI(RPGState.PlayerState playerState) {
        var context = new StringBuilder();
        context.append("CURRENT GAME STATE:\n");
        context.append("- Player Location: ").append(playerState.currentLocationId()).append("\n");
        context.append("- Player Health: ").append(playerState.health()).append("/100\n");
        context.append("- Active Quests: ").append(playerState.activeQuests().size()).append("\n");
        context.append("- Known NPCs: ").append(playerState.relationships().size()).append("\n");
        context.append("- Total Actions Taken: ").append(playerState.actionHistory().size()).append("\n\n");
        
        if (!playerState.actionHistory().isEmpty()) {
            context.append("RECENT ACTIONS:\n");
            playerState.actionHistory().stream()
                .skip(Math.max(0, playerState.actionHistory().size() - 3))
                .forEach(action -> {
                    context.append("- ").append(action.actionType())
                           .append(" ").append(action.target())
                           .append(" (").append(action.outcome()).append(")\n");
                });
            context.append("\n");
        }
        
        if (!playerState.relationships().isEmpty()) {
            context.append("NPC RELATIONSHIPS:\n");
            playerState.relationships().forEach((npcId, rel) -> {
                context.append("- ").append(npcId)
                       .append(": ").append(rel.relationType())
                       .append(" (trust: ").append(rel.trustLevel()).append(")\n");
            });
        }
        
        return context.toString();
    }
    
    private com.eventsourcing.core.domain.CommandResult<RPGEvent> processCommandBasedOnType(
            String playerId, String command, RPGState.PlayerState state) {
        return switch (command.toLowerCase()) {
            case "/look around", "/look" -> 
                RPGBusinessLogic.performAction(state, new RPGCommand.PerformAction(
                    UUID.randomUUID().toString(), playerId, "explore", "environment", 
                    Map.of("action", "look"), Instant.now()
                ));
            
            case "/attack goblin" -> 
                RPGBusinessLogic.performAction(state, new RPGCommand.PerformAction(
                    UUID.randomUUID().toString(), playerId, "combat", "goblin",
                    Map.of("action", "attack"), Instant.now()
                ));
            
            case "/talk tavern_keeper" -> 
                RPGBusinessLogic.initiateConversation(state, new RPGCommand.InitiateConversation(
                    UUID.randomUUID().toString(), playerId, "tavern_keeper", "greeting", Instant.now()
                ));
            
            case "/examine chest" -> 
                RPGBusinessLogic.performAction(state, new RPGCommand.PerformAction(
                    UUID.randomUUID().toString(), playerId, "examine", "chest",
                    Map.of("action", "examine"), Instant.now()
                ));
            
            default -> 
                RPGBusinessLogic.performAction(state, new RPGCommand.PerformAction(
                    UUID.randomUUID().toString(), playerId, "unknown", "unknown",
                    Map.of("command", command), Instant.now()
                ));
        };
    }
    
    private String generateAIPrompt(String playerId) {
        var playerState = commandHandler.getPlayerState(playerId);
        var context = generateGameContextForAI(playerState);
        
        return "🎭 CLAUDE AI GAME MASTER CONTEXT\n\n" + context + 
               "\nCONFIGURATION:\n" +
               "- AI Service: " + (aiService.isConfigured() ? "Claude AI" : "Simulation Mode") + "\n" +
               "- Event Sourcing: Active\n" +
               "- Persistent Context: Enabled\n\n" +
               "This context is used to generate intelligent, personalized responses " +
               "that consider the complete player journey and world state.";
    }
    
    private Map<String, Object> buildGameContext(RPGState.PlayerState playerState, AIResponse aiResponse) {
        var context = new HashMap<String, Object>();
        context.put("location", playerState.currentLocationId());
        context.put("health", playerState.health());
        context.put("active_quests", playerState.activeQuests().size());
        context.put("relationships", playerState.relationships().size());
        context.put("ai_powered", aiResponse.isSuccess());
        context.put("tokens_used", aiResponse.getTotalTokens());
        context.put("response_source", aiResponse.isSuccess() ? "claude-ai" : "fallback");
        return context;
    }
    
    private Map<String, Object> buildContextSummary(RPGState.PlayerState playerState) {
        return Map.of(
            "current_location", playerState.currentLocationId(),
            "player_health", playerState.health(),
            "total_actions", playerState.actionHistory().size(),
            "active_quests", playerState.activeQuests(),
            "npcs_met", playerState.relationships().keySet(),
            "ai_service_configured", aiService.isConfigured()
        );
    }
    
    // Utility methods remain the same
    private <T> T parseRequest(HttpExchange exchange, Class<T> clazz) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return objectMapper.readValue(is, clazz);
        }
    }
    
    private String getQueryParam(HttpExchange exchange, String param) {
        var query = exchange.getRequestURI().getQuery();
        if (query == null) return null;
        
        var params = query.split("&");
        for (var p : params) {
            var kv = p.split("=", 2);
            if (kv.length == 2 && param.equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }
    
    private void sendJsonResponse(HttpExchange exchange, Object response, int statusCode) throws IOException {
        var json = objectMapper.writeValueAsString(response);
        var bytes = json.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    private void sendErrorResponse(HttpExchange exchange, String message, int statusCode) throws IOException {
        var response = new GameResponse(false, null, null, null, message);
        sendJsonResponse(exchange, response, statusCode);
    }
    
    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(405, 0);
        exchange.close();
    }
}
