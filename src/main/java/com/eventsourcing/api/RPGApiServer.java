package com.eventsourcing.api;

import com.eventsourcing.rpg.*;
import com.eventsourcing.core.infrastructure.InMemoryEventStore;
import com.eventsourcing.ai.*;
import com.eventsourcing.gameSystem.core.*;
import com.eventsourcing.gameSystem.context.LocationContextManager;
import com.eventsourcing.gameSystem.context.GenericGameContextManager;
import com.eventsourcing.gameSystem.context.GameContextManagerFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    private final LocationContextManager locationContextManager;
    private final String aiLanguage;
    private final GenericGameContextManager genericGameContextManager;
    
    // CORS configuration
    private static final String FRONTEND_ORIGIN = "https://ai-rpg-web-09a84ac28189.herokuapp.com";
    private static final String ALLOWED_METHODS = "GET, POST, OPTIONS";
    private static final String ALLOWED_HEADERS = "Content-Type, Authorization";

    public RPGApiServer(int port) throws IOException {
        // Load environment variables
        EnvLoader.loadDotEnv();
        
        this.commandHandler = new RPGCommandHandler();
        
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
        
        // Initialize location context manager with adventure data and command handler
        this.locationContextManager = new LocationContextManager(currentAdventure, commandHandler);
        
        // Initialize GenericGameContextManager for state persistence
        this.genericGameContextManager = GameContextManagerFactory.createTSRBasicDnDManager(aiService);
        
        // Connect the command handler to the location context manager
        commandHandler.setLocationContextManager(locationContextManager);
        
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
        // Log all valid location IDs at startup
        log.info("[STARTUP] Valid location IDs: {}", currentAdventure.locations().keySet());
        
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
        server.createContext("/api/ai/prompt", new AIPromptKISSHandler());
        server.createContext("/api/metrics", new MetricsHandler());
        server.createContext("/api/game/metadata", exchange -> {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                setCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            setCorsHeaders(exchange);
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
                setCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
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
                setCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            setCorsHeaders(exchange);
            try {
                var request = parseRequest(exchange, SessionCreateRequest.class);
                
                if (request.playerId() == null || request.playerName() == null) {
                    sendErrorResponse(exchange, "PlayerID and PlayerName are required", 400);
                    return;
                }
                
                var sessionId = UUID.randomUUID().toString();
                activeSessions.put(sessionId, request.playerId());
                
                // Create player using KISS business logic
                var playerState = RPGBusinessLogic.createPlayer(request.playerId(), request.playerName());
                commandHandler.putPlayerState(request.playerId(), playerState);
                
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
                setCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            setCorsHeaders(exchange);
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
                setCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            setCorsHeaders(exchange);
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
                log.info("[STATUS] Fetching context for playerId: {} (sessionId: {})", playerId, sessionId);
                var context = genericGameContextManager.getCurrentContext();
                log.info("[STATUS] Context fetched: {}", context.toPrettyString());
                var response = new ApiModels.GameResponse(
                    true,
                    "World state retrieved from persistent state file",
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
    
    // Metrics Handler with AI metrics
    private class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                setCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            setCorsHeaders(exchange);
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
    
    // Normalize natural language commands to structured commands
    private String normalizeCommand(String command) {
        String trimmed = command.trim().toLowerCase();
        // Match common movement phrases
        // Examples: 'go north', 'move to cave', 'i go to the cave', 'enter dungeon', 'walk east'
        String[] movementVerbs = {"go", "move", "walk", "head", "enter", "proceed", "travel"};
        for (String verb : movementVerbs) {
            if (trimmed.startsWith("i " + verb + " ")) {
                trimmed = trimmed.substring(2); // Remove 'i '
            }
            if (trimmed.startsWith(verb + " ")) {
                // Remove verb and possible prepositions
                String rest = trimmed.substring(verb.length()).trim();
                rest = rest.replaceFirst("^(to |into |towards |the )", "");
                // Only allow single-word or hyphen/underscore location ids for now
                String[] words = rest.split(" ");
                if (words.length >= 1 && words[0].matches("[a-z0-9_-]+")) {
                    return "/go " + words[0];
                }
                // If multi-word, join as one id (e.g. 'dark-cave')
                if (words.length > 1) {
                    String loc = String.join("-", words);
                    return "/go " + loc;
                }
            }
        }
        return command;
    }
    
    // Use Claude to extract/translate a structured command from user input, with valid location IDs
    private String extractCommandWithClaude(String userMessage) {
        // Get valid location IDs from the current adventure
        String validLocations = String.join(", ", currentAdventure.locations().keySet());
        String prompt = "Convert this user message into a structured RPG command (e.g., /go cave, /attack goblin, /take sword). " +
                        "For movement commands, use ONLY the following location IDs: [" + validLocations + "] (case-sensitive). " +
                        "If it is not a command, return the original message.\n\nMessage: '" + userMessage + "'";
        // Use ClaudeAIService to get the result (synchronous call)
        AIResponse aiResponse = aiService.generateGameMasterResponse(prompt, "command extraction");
        if (aiResponse instanceof AIResponse.Success success) {
            return success.content().trim();
        } else if (aiResponse instanceof AIResponse.Fallback fallback) {
            return fallback.content().trim();
        } else if (aiResponse instanceof AIResponse.Error error) {
            // If error, just return the original message (minimal fallback)
            return userMessage;
        }
        return userMessage;
    }
    
    // KISS version of processGameActionWithAI
    private ApiModels.GameResponse processGameActionWithAI(String playerId, String command) {
        var startTime = System.currentTimeMillis();
        String sessionId = null;
        for (var entry : activeSessions.entrySet()) {
            if (entry.getValue().equals(playerId)) {
                sessionId = entry.getKey();
                break;
            }
        }
        try {
            // Use Claude to extract/translate the command
            String extractedCommand = extractCommandWithClaude(command);
            String normalizedCommand = normalizeCommand(extractedCommand).trim().toLowerCase();
            log.info("[COMMAND DEBUG] Original Message: '{}' | Extracted Message by Claude: '{}' | Normalized Message: '{}'", command, extractedCommand, normalizedCommand);
            
            // Parse the command more reliably
            CommandInfo parsedCommand = parseCommand(normalizedCommand);
            log.info("[PARSE DEBUG] Command: '{}', Type: '{}', Target: '{}'", normalizedCommand, parsedCommand.type(), parsedCommand.target());
            log.info("[COMMAND PARSED] Type: {}, Target: {}, Parameters: {}", parsedCommand.type(), parsedCommand.target(), parsedCommand.parameters());
            
            // Get current player state
            var playerState = commandHandler.getPlayerState(playerId);
            
            // Handle different command types
            switch (parsedCommand.type()) {
                case "go" -> {
                    String toLocationId = parsedCommand.target();
                    log.info("[MOVE] Player {} moving from {} to {}", playerId, playerState.currentLocationId(), toLocationId);
                    playerState = RPGBusinessLogic.movePlayer(playerState, toLocationId);
                    commandHandler.putPlayerState(playerId, playerState);
                    // Persist new location to in-memory state
                    ObjectNode changes = objectMapper.createObjectNode();
                    changes.put("player_location", toLocationId);
                    log.info("[MOVE] Persisting new player_location to in-memory state: {}", toLocationId);
                    genericGameContextManager.updateState(changes);
                }
                case "heal" -> {
                    try {
                        int amount = Integer.parseInt(parsedCommand.target());
                        int newHealth = Math.min(100, playerState.health() + amount);
                        playerState = RPGBusinessLogic.changePlayerHealth(playerState, newHealth);
                        commandHandler.putPlayerState(playerId, playerState);
                    } catch (NumberFormatException e) {
                        log.warn("[KISS] Invalid heal amount: {}", parsedCommand.target());
                    }
                }
                case "skill" -> {
                    String[] parts = parsedCommand.target().split(" ");
                    if (parts.length == 2) {
                        String skillName = parts[0];
                        try {
                            int level = Integer.parseInt(parts[1]);
                            playerState = RPGBusinessLogic.addPlayerSkill(playerState, skillName, level);
                            commandHandler.putPlayerState(playerId, playerState);
                        } catch (NumberFormatException e) {
                            log.warn("[KISS] Invalid skill level: {}", parsedCommand.target());
                        }
                    }
                }
                case "startquest" -> {
                    String questId = parsedCommand.target();
                    playerState = RPGBusinessLogic.startQuest(playerState, questId);
                    commandHandler.putPlayerState(playerId, playerState);
                }
                case "completequest" -> {
                    String questId = parsedCommand.target();
                    playerState = RPGBusinessLogic.completeQuest(playerState, questId);
                    commandHandler.putPlayerState(playerId, playerState);
                }
                case "relationship" -> {
                    String[] parts = parsedCommand.target().split(" ");
                    if (parts.length == 2) {
                        String npcId = parts[0];
                        String relationType = parts[1];
                        playerState = RPGBusinessLogic.addRelationship(playerState, npcId, relationType);
                        commandHandler.putPlayerState(playerId, playerState);
                    }
                }
                case "action" -> {
                    String[] parts = parsedCommand.target().split(" ");
                    if (parts.length >= 4) {
                        String actionType = parts[0];
                        String target = parts[1];
                        String outcome = parts[2];
                        String locationId = parts[3];
                        playerState = RPGBusinessLogic.addAction(playerState, actionType, target, outcome, locationId);
                        commandHandler.putPlayerState(playerId, playerState);
                    }
                }
                default -> {
                    log.info("[COMMAND] Unknown command type: {}, treating as natural language", parsedCommand.type());
                }
            }
            
            // Get updated player state
            var gameContext = generateGameContextForAI(playerState);
            String languageInstruction = "";
            if ("it".equalsIgnoreCase(aiLanguage)) {
                languageInstruction = "Rispondi sempre in italiano.";
            }
            var aiResponse = aiService.generateGameMasterResponse(gameContext, command + (languageInstruction.isEmpty() ? "" : ("\n" + languageInstruction)));
            var context = buildGameContext(playerState, aiResponse);
            boolean aiOverloaded = false;
            String userMessage;
            switch (aiResponse) {
                case AIResponse.Success success -> userMessage = success.content();
                case AIResponse.Fallback fallback -> userMessage = fallback.content();
                case AIResponse.Error error -> {
                    if (error.errorMessage() != null && error.errorMessage().toLowerCase().contains("overloaded")) {
                        userMessage = "⚠️ The AI Dungeon Master is currently overloaded. You are seeing a fallback response or may need to try again in a moment.";
                        aiOverloaded = true;
                    } else {
                        userMessage = "An unexpected error occurred: " + error.errorMessage();
                    }
                }
                default -> userMessage = "Unknown AI response.";
            }
            if (context != null) {
                context.put("ai_overloaded", aiOverloaded);
            }
            metrics.recordResponseTime(System.currentTimeMillis() - startTime);
            return new ApiModels.GameResponse(
                true,
                userMessage,
                sessionId,
                context,
                null
            );
        } catch (Exception e) {
            return new ApiModels.GameResponse(
                false,
                null,
                sessionId,
                null,
                "Failed to process action: " + e.getMessage()
            );
        }
    }
    
    /**
     * Parse a command string into structured command information
     */
    private CommandInfo parseCommand(String command) {
        String trimmed = command.trim();
        log.info("[PARSE DEBUG] (parseCommand) trimmed command: '{}'", trimmed);
        
        // First, try to extract embedded commands from AI responses
        String extractedCommand = extractEmbeddedCommand(trimmed);
        if (extractedCommand != null) {
            log.info("[PARSE DEBUG] (parseCommand) Extracted embedded command: '{}'", extractedCommand);
            trimmed = extractedCommand;
        }
        
        // Handle commands that start with /
        if (trimmed.startsWith("/")) {
            // Remove the first / and any leading whitespace
            String afterSlash = trimmed.substring(1).trim();
            
            // Split on first whitespace to get command type and target
            String[] parts = afterSlash.split("\\s+", 2);
            String commandType = parts[0].toLowerCase();
            String target = parts.length > 1 ? parts[1] : "";
            
            log.info("[PARSE DEBUG] (parseCommand) Raw: '{}', AfterSlash: '{}', Type: '{}', Target: '{}'", 
                command, afterSlash, commandType, target);
            
            // Validate command type is not empty
            if (commandType.isEmpty()) {
                log.warn("[PARSE DEBUG] Empty command type detected, treating as natural language");
                return new CommandInfo("natural", trimmed, Map.of());
            }
            
            return new CommandInfo(commandType, target, Map.of());
        }
        
        // Handle natural language commands (no / prefix)
        log.info("[PARSE DEBUG] (parseCommand) Raw: '{}', Type: 'natural', Target: '{}'", command, trimmed);
        return new CommandInfo("natural", trimmed, Map.of());
    }
    
    /**
     * Extract embedded commands from AI responses
     * Looks for patterns like bold markdown commands or slash commands within the text
     */
    private String extractEmbeddedCommand(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        // Pattern 1: Look for **/command** (bold markdown with command)
        var boldPattern = java.util.regex.Pattern.compile("\\*\\*/([^\\*]+)\\*\\*");
        var boldMatcher = boldPattern.matcher(text);
        if (boldMatcher.find()) {
            String command = boldMatcher.group(1).trim();
            log.info("[PARSE DEBUG] (extractEmbeddedCommand) Found bold command: '{}'", command);
            return "/" + command;
        }
        
        // Pattern 2: Look for /command within the text (not at start)
        var slashPattern = java.util.regex.Pattern.compile("\\s+/([^\\s]+(?:\\s+[^\\s]+)*)");
        var slashMatcher = slashPattern.matcher(text);
        if (slashMatcher.find()) {
            String command = slashMatcher.group(1).trim();
            log.info("[PARSE DEBUG] (extractEmbeddedCommand) Found embedded slash command: '{}'", command);
            return "/" + command;
        }
        
        // Pattern 3: Look for "command" in quotes
        var quotePattern = java.util.regex.Pattern.compile("\"/([^\"]+)\"");
        var quoteMatcher = quotePattern.matcher(text);
        if (quoteMatcher.find()) {
            String command = quoteMatcher.group(1).trim();
            log.info("[PARSE DEBUG] (extractEmbeddedCommand) Found quoted command: '{}'", command);
            return "/" + command;
        }
        
        log.info("[PARSE DEBUG] (extractEmbeddedCommand) No embedded command found in text");
        return null;
    }
    
    /**
     * Command information record
     */
    private record CommandInfo(String type, String target, Map<String, String> parameters) {}
    
    private String generateGameContextForAI(RPGState.PlayerState playerState) {
        // Use the enhanced location context manager for rich context
        String baseContext = generateBasicGameContext(playerState);
        
        return locationContextManager.generateEnhancedAIPrompt(playerState, baseContext);
    }
    
    /**
     * Generate basic game context without location enhancement (for fallback)
     */
    private String generateBasicGameContext(RPGState.PlayerState playerState) {
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
        log.info("[CONTEXT SUMMARY] Current location: {}", currentLocation);
        if (currentLocation == null || currentLocation.isEmpty()) {
            currentLocation = "village";
        }
        // Get enhanced location context
        var locationContext = locationContextManager.getFullLocationContext(currentLocation, playerState.playerId());
        Map<String, Object> context = new HashMap<>();
        context.put("current_location", currentLocation);
        context.put("location_name", locationContext.name());
        context.put("location_type", locationContext.type());
        context.put("location_features", locationContext.features());
        context.put("available_exits", locationContext.connections().keySet());
        context.put("requires_light", locationContext.requiresLight());
        context.put("has_been_explored", locationContext.hasBeenExplored());
        context.put("player_health", playerState.health());
        context.put("total_actions", playerState.actionHistory().size());
        context.put("active_quests", playerState.activeQuests());
        context.put("npcs_met", playerState.relationships().keySet());
        context.put("ai_service_configured", aiService.isConfigured());
        context.put("location_context_cache_size", locationContextManager.getCacheSize());
        return context;
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
    
    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", FRONTEND_ORIGIN);
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", ALLOWED_METHODS);
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", ALLOWED_HEADERS);
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
    }

    private void sendJsonResponse(HttpExchange exchange, Object response, int statusCode) throws IOException {
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        var responseBody = objectMapper.writeValueAsBytes(response);
        exchange.sendResponseHeaders(statusCode, responseBody.length);
        try (var os = exchange.getResponseBody()) {
            os.write(responseBody);
        }
    }
    
    private void sendErrorResponse(HttpExchange exchange, String message, int statusCode) throws IOException {
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        var response = Map.of("success", false, "error", message);
        var responseBody = objectMapper.writeValueAsBytes(response);
        exchange.sendResponseHeaders(statusCode, responseBody.length);
        try (var os = exchange.getResponseBody()) {
            os.write(responseBody);
        }
    }
    
    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().add("Allow", "POST, OPTIONS");
        exchange.sendResponseHeaders(405, -1);
        exchange.close();
    }

    // KISS AI Prompt Handler
    private class AIPromptKISSHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                setCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }
            setCorsHeaders(exchange);
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
                log.info("[PROMPT] Fetching context for playerId: {} (sessionId: {})", playerId, sessionId);
                var context = genericGameContextManager.getCurrentContext();
                log.info("[PROMPT] Context fetched: {}", context.toPrettyString());
                var response = new ApiModels.GameResponse(
                    true,
                    context.toPrettyString(),
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
}
