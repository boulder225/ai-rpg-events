package com.eventsourcing.api;

import com.eventsourcing.rpg.*;
import com.eventsourcing.core.infrastructure.InMemoryEventStore;
import com.eventsourcing.ai.*;
import com.eventsourcing.gameSystem.core.*;
import com.eventsourcing.logging.RPGLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.eventsourcing.api.ApiModels.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Complete RESTful API server for the AI-RPG platform with Claude AI integration.
 */
public class RPGApiServer {
    
    private static final Logger log = LoggerFactory.getLogger(RPGApiServer.class);
    
    private final HttpServer server;
    private final RPGCommandHandler commandHandler;
    private final ClaudeAIService aiService;
    private final ObjectMapper objectMapper;
    private final Map<String, String> activeSessions;
    private final RPGMetrics metrics;
    private final GameSystem gameSystem;
    private final AdventureData currentAdventure;
    private final String aiLanguage;
    
    public RPGApiServer(int port) throws IOException {
        // Load environment variables
        EnvLoader.loadDotEnv();
        
        var eventStore = new InMemoryEventStore<RPGEvent>();
        this.commandHandler = new RPGCommandHandler(eventStore);
        
        // Initialize Claude AI service
        var aiConfig = AIConfig.fromEnvironment();
        this.aiService = new ClaudeAIService(aiConfig);
        
        this.objectMapper = new ObjectMapper();
        this.activeSessions = new HashMap<>();
        this.metrics = new RPGMetrics();
        
        // Initialize game system from configuration
        Properties defaultConfig = new Properties();
        defaultConfig.setProperty("game.system", "dnd");
        defaultConfig.setProperty("game.adventure", "tsr_basic");
        this.gameSystem = GameSystemFactory.createFromConfig(defaultConfig);
        this.currentAdventure = gameSystem.loadAdventure(defaultConfig.getProperty("game.adventure", "tsr_basic"));
        // Language config (env or system property, default 'en')
        this.aiLanguage = System.getenv().getOrDefault("AI_LANGUAGE", System.getProperty("ai.language", "en"));
        
        log.info("🎮 Initializing AI-RPG Platform on port {}", port);
        log.info("Game System loaded: name={}, adventure={}, locations={}, npcs={}, encounters={}", 
            gameSystem.getSystemName(),
            currentAdventure.title(),
            currentAdventure.locations().size(),
            currentAdventure.npcs().size(),
            currentAdventure.encounters().size());
        
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        setupRoutes();
        server.setExecutor(Executors.newFixedThreadPool(10));
        
        // Log AI configuration status
        if (aiService.isConfigured()) {
            log.info("🤖 Claude AI integration ENABLED: model={}", aiConfig.claudeModel());
            if (aiConfig.claudeModel().contains("claude-4") || aiConfig.claudeModel().contains("sonnet-4") || aiConfig.claudeModel().contains("opus-4")) {
                log.info("🎆 CLAUDE 4 DETECTED - Latest AI power activated!");
            }
        } else {
            log.warn("⚠️ Claude AI integration DISABLED - using fallback responses");
            log.info("📝 Set CLAUDE_API_KEY in .env file to enable Claude AI responses");
        }
    }
    
    private void setupRoutes() {
        server.createContext("/api/session/create", new CreateSessionHandler());
        server.createContext("/api/game/action", new GameActionHandler());
        server.createContext("/api/game/status", new GameStatusHandler());
        server.createContext("/api/ai/prompt", new AIPromptHandler());
        server.createContext("/api/metrics", new MetricsHandler());
        server.createContext("/api/game/metadata", exchange -> {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
                return;
            }
            var metadata = gameSystem.getMetadata();
            var json = objectMapper.writeValueAsString(metadata);
            byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        // Global OPTIONS handler for CORS
        server.createContext("/", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }
            // Optionally, handle root or fallback
            exchange.sendResponseHeaders(404, 0);
            exchange.close();
        });
        // Removed web interface handler - now using React frontend
    }
    
    public void start() {
        server.start();
        log.info("🚀 AI-RPG Server started successfully on port {}", server.getAddress().getPort());
        log.info("🌐 API endpoints available at http://localhost:{}", server.getAddress().getPort());
        log.info("📚 Ready for {} adventures!", aiService.isConfigured() ? "intelligent AI" : "simulated AI");
        
        if (log.isInfoEnabled()) {
            log.info("📚 API Documentation:");
            log.info("  POST /api/session/create - Create new adventure session");
            log.info("  POST /api/game/action - Execute game actions with AI responses");
            log.info("  GET  /api/game/status - Get complete world state");
            log.info("  GET  /api/ai/prompt - View AI context prompt");
            log.info("  GET  /api/metrics - System performance metrics");
            log.info("  GET  /api/game/metadata - Game system configuration");
        }
    }
    
    public void stop() {
        server.stop(0);
        log.info("🛑 AI-RPG Server stopped");
    }
    
    // Session Creation Handler
    private class CreateSessionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            
            try {
                var request = parseRequest(exchange, SessionCreateRequest.class);
                
                if (request.playerId() == null || request.playerName() == null) {
                    sendErrorResponse(exchange, "PlayerID and PlayerName are required", 400);
                    return;
                }
                
                var sessionId = UUID.randomUUID().toString();
                activeSessions.put(sessionId, request.playerId());
                
                // Create player in event store
                commandHandler.executePlayerCommand(request.playerId(), playerState -> 
                    RPGBusinessLogic.createPlayer(new RPGCommand.CreatePlayer(
                        UUID.randomUUID().toString(),
                        request.playerId(),
                        request.playerName(),
                        Instant.now()
                    ))
                );
                
                metrics.incrementSessions();
                
                // Generate contextual welcome with full adventure knowledge
                var adventureContext = gameSystem.getAdventureContext(currentAdventure, "village");
                String languageInstruction = "";
                if ("it".equalsIgnoreCase(aiLanguage)) {
                    languageInstruction = "Rispondi sempre in italiano.";
                }
                var welcomePrompt = String.format(
                    "New player %s has just arrived in their home village. They are a brave fighter " +
                    "seeking the bandit Bargle who has been terrorizing the area. Full context: %s%s", 
                    request.playerName(), adventureContext, languageInstruction.isEmpty() ? "" : ("\n" + languageInstruction));
                var aiResponse = aiService.generateGameMasterResponse(welcomePrompt, "starting TSR Basic D&D adventure");
                
                var welcomeMessage = switch (aiResponse) {
                    case AIResponse.Success success -> success.content();
                    case AIResponse.Fallback fallback -> fallback.content();
                    case AIResponse.Error error -> 
                        String.format("🌟 Benvenuto, %s! La tua avventura inizia in un villaggio mistico dove la magia antica scorre attraverso strade di ciottoli.", request.playerName());
                };
                
                var response = new ApiModels.GameResponse(
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
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            
            try {
                var request = parseRequest(exchange, GameActionRequest.class);
                
                if (request.sessionId() == null || request.command() == null) {
                    sendErrorResponse(exchange, "SessionID and Command are required", 400);
                    return;
                }
                
                var playerId = activeSessions.get(request.sessionId());
                if (playerId == null) {
                    sendErrorResponse(exchange, "Session not found", 404);
                    return;
                }
                
                var result = processGameActionWithAI(playerId, request.command());
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
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }
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
                
                var response = new ApiModels.GameResponse(
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
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }
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
                
                var response = new ApiModels.GameResponse(
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
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }
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
            
            var response = new ApiModels.GameResponse(
                true,
                "System metrics retrieved",
                null,
                metricsData,
                null
            );
            
            sendJsonResponse(exchange, response, 200);
        }
    }
    
    // Core game processing with Claude AI
    private ApiModels.GameResponse processGameActionWithAI(String playerId, String command) {
        var startTime = System.currentTimeMillis();
        
        try {
            var playerState = commandHandler.getPlayerState(playerId);
            
            // Generate full context for AI
            var gameContext = generateGameContextForAI(playerState);
            
            // Get sessionId from activeSessions (reverse lookup)
            String sessionId = null;
            for (var entry : activeSessions.entrySet()) {
                if (entry.getValue().equals(playerId)) {
                    sessionId = entry.getKey();
                    break;
                }
            }
            String languageInstruction = "";
            if ("it".equalsIgnoreCase(aiLanguage)) {
                languageInstruction = "Rispondi sempre in italiano.";
            }
            var aiResponse = aiService.generateGameMasterResponse(gameContext, command + (languageInstruction.isEmpty() ? "" : ("\n" + languageInstruction)));
            
            // Record AI token usage if available
            switch (aiResponse) {
                case AIResponse.Success success -> {
                    aiService.getMetrics().recordTokenUsage(success.getTotalTokens());
                }
                case AIResponse.Fallback fallback -> {
                    // No token usage for fallback responses
                }
                case AIResponse.Error error -> {
                    // No token usage for error responses
                }
            }
            
            // Process command and record events
            commandHandler.executePlayerCommand(playerId, state -> {
                return processCommandBasedOnType(playerId, command, state);
            });
            
            var updatedState = commandHandler.getPlayerState(playerId);
            var context = buildGameContext(updatedState, aiResponse);
            
            metrics.recordResponseTime(System.currentTimeMillis() - startTime);
            
            return new ApiModels.GameResponse(
                true, 
                aiResponse.content(), 
                null, 
                context, 
                null
            );
            
        } catch (Exception e) {
            return new ApiModels.GameResponse(
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
        
        // Add TSR Basic D&D Adventure context first
        context.append("=== TSR BASIC D&D ADVENTURE CONTEXT ===\n");
        context.append(gameSystem.getAdventureContext(currentAdventure, 
            playerState.currentLocationId() != null ? playerState.currentLocationId() : "village")).append("\n");
        
        // Add character context
        context.append("=== CHARACTER STATUS ===\n");
        context.append("- Player Location: ").append(playerState.currentLocationId() != null ? playerState.currentLocationId() : "village").append("\n");
        context.append("- Player Health: ").append(playerState.health()).append("/8 hp\n");
        context.append("- Character Class: Fighter\n");
        context.append("- Equipment: Chain Mail (AC 4), Sword, Dagger, Lantern\n");
        context.append("- Ability Scores: STR 17 (+2), DEX 11, INT 9, WIS 8, CON 16, CHA 14\n");
        context.append("- Active Quests: Find Bargle the bandit\n");
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
            context.append("\n");
        }
        
        // Add D&D rules context
        context.append(gameSystem.getRulesContext());
        
        return context.toString();
    }
    
    private com.eventsourcing.core.domain.CommandResult<RPGEvent> processCommandBasedOnType(
            String playerId, String command, RPGState.PlayerState state) {
        // Generic handler: if command is '/go <locationId>', treat as move
        String trimmed = command.trim().toLowerCase();
        if (trimmed.startsWith("/go ")) {
            String toLocationId = trimmed.substring(4).trim().toLowerCase();
            if (!toLocationId.isEmpty()) {
                return RPGBusinessLogic.movePlayer(state, new RPGCommand.MovePlayer(
                    UUID.randomUUID().toString(),
                    playerId,
                    toLocationId,
                    Instant.now()
                ));
            }
        }
        // Otherwise, treat as generic action
        return RPGBusinessLogic.performAction(state, new RPGCommand.PerformAction(
            UUID.randomUUID().toString(), playerId, "unknown", "unknown",
            Map.of("command", command), Instant.now()
        ));
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
        context.put("response_source", aiResponse.isSuccess() ? "claude-ai" : "fallback");
        
        // Add token usage only for successful responses
        switch (aiResponse) {
            case AIResponse.Success success -> {
                context.put("tokens_used", success.getTotalTokens());
            }
            case AIResponse.Fallback fallback -> {
                context.put("tokens_used", 0);
                context.put("fallback_reason", fallback.reason());
            }
            case AIResponse.Error error -> {
                context.put("tokens_used", 0);
                context.put("error_message", error.errorMessage());
            }
        }
        
        return context;
    }
    
    private Map<String, Object> buildContextSummary(RPGState.PlayerState playerState) {
        // Default to village if no location is set
        String currentLocation = playerState.currentLocationId();
        if (currentLocation == null || currentLocation.isEmpty()) {
            currentLocation = "village";
        }
        // Add adventure context (location description, features, etc.)
        String adventureContext = gameSystem.getAdventureContext(currentAdventure, currentLocation);
        return Map.of(
            "current_location", currentLocation,
            "player_health", playerState.health(),
            "total_actions", playerState.actionHistory().size(),
            "active_quests", playerState.activeQuests(),
            "npcs_met", playerState.relationships().keySet(),
            "ai_service_configured", aiService.isConfigured(),
            "adventure_context", adventureContext
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
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        var responseBody = objectMapper.writeValueAsBytes(response);
        exchange.sendResponseHeaders(statusCode, responseBody.length);
        try (var os = exchange.getResponseBody()) {
            os.write(responseBody);
        }
    }
    
    private void sendErrorResponse(HttpExchange exchange, String message, int statusCode) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        var response = Map.of("success", false, "error", message);
        var responseBody = objectMapper.writeValueAsBytes(response);
        exchange.sendResponseHeaders(statusCode, responseBody.length);
        try (var os = exchange.getResponseBody()) {
            os.write(responseBody);
        }
    }
    
    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Allow", "POST, OPTIONS");
        exchange.sendResponseHeaders(405, -1);
        exchange.close();
    }
}
