package com.eventsourcing.gameSystem.context;

import com.eventsourcing.gameSystem.plugins.GameSystemPlugin;
import com.eventsourcing.gameSystem.plugins.ValidationResult;
import com.eventsourcing.ai.ClaudeAIService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

/**
 * Generic context manager that works with any game system through plugins.
 * Maintains world state consistency and handles AI interactions.
 */
public class GenericGameContextManager {
    
    private static final Logger LOGGER = Logger.getLogger(GenericGameContextManager.class.getName());
    
    private final ObjectMapper objectMapper;
    private final ClaudeAIService aiService;
    private final List<GameSystemPlugin> plugins;
    private final String gameDataFile;
    
    private JsonNode gameData;
    private JsonNode state;
    
    public GenericGameContextManager(String gameDataFile, ClaudeAIService aiService) {
        this.objectMapper = new ObjectMapper();
        this.aiService = aiService;
        this.plugins = new ArrayList<>();
        this.gameDataFile = gameDataFile;
        loadGameData();
        this.state = createInitialState();
    }
    
    /**
     * Register a game system plugin
     */
    public void registerPlugin(GameSystemPlugin plugin) {
        plugins.add(plugin);
        LOGGER.info("Registered plugin: " + plugin.getName() + " v" + plugin.getVersion());
    }
    
    /**
     * Get current game context with plugin enhancements
     */
    public JsonNode getCurrentContext() {
        ObjectNode baseContext = objectMapper.createObjectNode();
        
        // Build base context
        baseContext.set("current_location", getCurrentLocation());
        baseContext.set("nearby_areas", getNearbyAreas());
        baseContext.set("character_state", getCharacterState());
        baseContext.set("recent_events", getRecentEvents(5));
        baseContext.set("world_state", getRelevantWorldState());
        baseContext.set("quest_progress", getQuestProgress());
        baseContext.put("timestamp", Instant.now().toString());
        
        // Allow plugins to enhance context
        JsonNode enhancedContext = baseContext;
        for (GameSystemPlugin plugin : plugins) {
            try {
                enhancedContext = plugin.enhanceContext(enhancedContext);
            } catch (Exception e) {
                LOGGER.warning("Plugin " + plugin.getName() + " failed to enhance context: " + e.getMessage());
            }
        }
        
        return enhancedContext;
    }
    
    /**
     * Validate an action against all registered plugins
     */
    public List<ValidationResult> validateAction(JsonNode action) {
        List<ValidationResult> results = new ArrayList<>();
        JsonNode currentContext = getCurrentContext();
        
        // Core validation first
        ValidationResult coreValidation = validateCoreRules(action, currentContext);
        results.add(coreValidation);
        
        // Plugin-specific validation
        for (GameSystemPlugin plugin : plugins) {
            try {
                ValidationResult pluginResult = plugin.validateAction(action, currentContext);
                results.add(pluginResult);
            } catch (Exception e) {
                LOGGER.warning("Plugin " + plugin.getName() + " validation failed: " + e.getMessage());
                results.add(ValidationResult.invalid("Plugin validation error: " + e.getMessage(), plugin.getName()));
            }
        }
        
        return results;
    }
    
    /**
     * Update game state with plugin hooks
     */
    public void updateState(JsonNode changes) {
        JsonNode currentContext = getCurrentContext();
        
        // Pre-update plugin hooks
        for (GameSystemPlugin plugin : plugins) {
            try {
                plugin.beforeStateUpdate(changes, currentContext);
            } catch (Exception e) {
                LOGGER.warning("Plugin " + plugin.getName() + " beforeStateUpdate failed: " + e.getMessage());
            }
        }
        
        // Apply core state changes
        applyCoreStateChanges(changes);
        
        // Get updated context for post-update hooks
        JsonNode newContext = getCurrentContext();
        
        // Post-update plugin hooks
        for (GameSystemPlugin plugin : plugins) {
            try {
                plugin.afterStateUpdate(changes, newContext);
            } catch (Exception e) {
                LOGGER.warning("Plugin " + plugin.getName() + " afterStateUpdate failed: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get AI response with full context and validation
     */
    public String getAIResponse(String userInput) throws IOException {
        JsonNode context = getCurrentContext();
        String prompt = buildPrompt(context, userInput);
        
        try {
            String response = aiService.generateResponse(prompt);
            
            // Parse response for state changes
            JsonNode stateChanges = parseStateChanges(response);
            if (stateChanges != null && !stateChanges.isEmpty()) {
                
                // Validate proposed changes
                List<ValidationResult> validationResults = validateAction(stateChanges);
                boolean allValid = validationResults.stream().allMatch(ValidationResult::isValid);
                
                if (!allValid) {
                    // Handle validation failures
                    return handleValidationFailure(validationResults, userInput);
                }
                
                // Apply validated changes
                updateState(stateChanges);
            }
            
            return cleanResponse(response);
            
        } catch (Exception e) {
            LOGGER.severe("AI response generation failed: " + e.getMessage());
            throw new IOException("Failed to get AI response", e);
        }
    }
    
    // Private helper methods
    
    private void loadGameData() {
        try {
            File file = new File(gameDataFile);
            gameData = objectMapper.readTree(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load game data from: " + gameDataFile, e);
        }
    }
    
    private JsonNode createInitialState() {
        ObjectNode initialState = objectMapper.createObjectNode();
        initialState.put("session_id", UUID.randomUUID().toString());
        initialState.put("created_at", Instant.now().toString());
        initialState.put("player_location", "village");
        initialState.set("world_state", objectMapper.createObjectNode());
        initialState.set("character_state", objectMapper.createObjectNode());
        initialState.set("quest_progress", objectMapper.createObjectNode());
        initialState.set("timeline", objectMapper.createArrayNode());
        return initialState;
    }
    
    private JsonNode getCurrentLocation() {
        return state.path("player_location");
    }
    
    private JsonNode getNearbyAreas() {
        String currentLoc = getCurrentLocation().asText();
        
        // Find connected locations from game data
        if (gameData.has("locations")) {
            for (JsonNode location : gameData.get("locations")) {
                if (location.get("id").asText().equals(currentLoc)) {
                    return location.path("connections");
                }
            }
        }
        
        return objectMapper.createArrayNode();
    }
    
    private JsonNode getCharacterState() {
        return state.path("character_state");
    }
    
    private JsonNode getRecentEvents(int count) {
        JsonNode timeline = state.path("timeline");
        if (timeline.isArray() && timeline.size() > 0) {
            // Return last 'count' events
            int start = Math.max(0, timeline.size() - count);
            ObjectNode result = objectMapper.createObjectNode();
            result.set("events", timeline);
            result.put("showing_last", count);
            return result;
        }
        return objectMapper.createArrayNode();
    }
    
    private JsonNode getRelevantWorldState() {
        return state.path("world_state");
    }
    
    private JsonNode getQuestProgress() {
        return state.path("quest_progress");
    }
    
    private ValidationResult validateCoreRules(JsonNode action, JsonNode context) {
        List<String> violations = new ArrayList<>();
        
        // Basic spatial logic validation
        if (action.has("movement")) {
            if (!validateSpatialMovement(action.get("movement"), context)) {
                violations.add("Invalid movement - no path exists between locations");
            }
        }
        
        // Basic inventory validation
        if (action.has("item_interaction")) {
            if (!validateItemInteraction(action.get("item_interaction"), context)) {
                violations.add("Invalid item interaction - item not accessible");
            }
        }
        
        return violations.isEmpty() ? 
            ValidationResult.valid("CoreRules") : 
            ValidationResult.invalid(violations, "CoreRules");
    }
    
    private boolean validateSpatialMovement(JsonNode movement, JsonNode context) {
        // Basic spatial validation - check if movement is to connected location
        String from = movement.path("from").asText();
        String to = movement.path("to").asText();
        
        JsonNode nearbyAreas = context.path("nearby_areas");
        if (nearbyAreas.isArray()) {
            for (JsonNode area : nearbyAreas) {
                if (area.asText().equals(to)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean validateItemInteraction(JsonNode interaction, JsonNode context) {
        // Basic item validation - check if item exists in current location
        String item = interaction.path("item").asText();
        String location = context.path("current_location").asText();
        
        JsonNode worldState = context.path("world_state");
        JsonNode locationState = worldState.path(location);
        JsonNode items = locationState.path("items");
        
        if (items.isArray()) {
            for (JsonNode locationItem : items) {
                if (locationItem.asText().equals(item)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void applyCoreStateChanges(JsonNode changes) {
        if (state instanceof ObjectNode) {
            ObjectNode mutableState = (ObjectNode) state;
            
            // Update timestamp
            mutableState.put("last_updated", Instant.now().toString());
            
            // Apply specific changes
            if (changes.has("player_location")) {
                mutableState.set("player_location", changes.get("player_location"));
            }
            
            if (changes.has("character_state")) {
                JsonNode charChanges = changes.get("character_state");
                mergeJsonNodes(mutableState.path("character_state"), charChanges);
            }
            
            if (changes.has("world_state")) {
                JsonNode worldChanges = changes.get("world_state");
                mergeJsonNodes(mutableState.path("world_state"), worldChanges);
            }
            
            // Add to timeline
            if (changes.has("event")) {
                addEventToTimeline(changes);
            }
        }
    }
    
    private void mergeJsonNodes(JsonNode target, JsonNode source) {
        if (target instanceof ObjectNode && source instanceof ObjectNode) {
            ObjectNode targetObj = (ObjectNode) target;
            source.fields().forEachRemaining(entry -> {
                targetObj.set(entry.getKey(), entry.getValue());
            });
        }
    }
    
    private void addEventToTimeline(JsonNode event) {
        if (state instanceof ObjectNode) {
            ObjectNode mutableState = (ObjectNode) state;
            if (!mutableState.has("timeline")) {
                mutableState.set("timeline", objectMapper.createArrayNode());
            }
            
            ObjectNode eventWithTimestamp = objectMapper.createObjectNode();
            eventWithTimestamp.put("timestamp", Instant.now().toString());
            eventWithTimestamp.setAll((ObjectNode) event);
            
            mutableState.withArray("timeline").add(eventWithTimestamp);
        }
    }
    
    private String buildPrompt(JsonNode context, String userInput) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("GAME CONTEXT:\n");
        prompt.append(context.toPrettyString());
        prompt.append("\n\nCONSISTENCY RULES:\n");
        prompt.append("- Maintain spatial logic\n");
        prompt.append("- Track inventory accurately\n");
        prompt.append("- Preserve character capabilities\n");
        prompt.append("- Follow established relationships\n");
        prompt.append("\nUSER INPUT: \"").append(userInput).append("\"\n\n");
        prompt.append("Respond as the game master, maintaining consistency with the context above.\n");
        prompt.append("Specify any state changes in JSON format after STATE_CHANGES:\n");
        
        // Allow plugins to enhance prompt
        String enhancedPrompt = prompt.toString();
        for (GameSystemPlugin plugin : plugins) {
            try {
                enhancedPrompt = plugin.enhancePrompt(enhancedPrompt, context);
            } catch (Exception e) {
                LOGGER.warning("Plugin " + plugin.getName() + " failed to enhance prompt: " + e.getMessage());
            }
        }
        
        return enhancedPrompt;
    }
    
    private JsonNode parseStateChanges(String response) {
        // Look for STATE_CHANGES: in the response and parse JSON after it
        int stateChangesIndex = response.indexOf("STATE_CHANGES:");
        if (stateChangesIndex != -1) {
            String jsonPart = response.substring(stateChangesIndex + "STATE_CHANGES:".length()).trim();
            try {
                return objectMapper.readTree(jsonPart);
            } catch (Exception e) {
                LOGGER.warning("Failed to parse state changes from AI response: " + e.getMessage());
            }
        }
        return null;
    }
    
    private String cleanResponse(String response) {
        // Remove STATE_CHANGES section from user-facing response
        int stateChangesIndex = response.indexOf("STATE_CHANGES:");
        if (stateChangesIndex != -1) {
            return response.substring(0, stateChangesIndex).trim();
        }
        return response;
    }
    
    private String handleValidationFailure(List<ValidationResult> validationResults, String userInput) {
        StringBuilder response = new StringBuilder();
        response.append("I need to check something about that action...\n\n");
        
        for (ValidationResult result : validationResults) {
            if (!result.isValid()) {
                response.append("⚠️ ").append(result.getPluginName()).append(" validation:\n");
                for (String violation : result.getViolations()) {
                    response.append("- ").append(violation).append("\n");
                }
                
                if (result.hasSuggestions()) {
                    response.append("\nSuggestions:\n");
                    for (String suggestion : result.getSuggestions()) {
                        response.append("- ").append(suggestion).append("\n");
                    }
                }
                response.append("\n");
            }
        }
        
        return response.toString();
    }
    
    // Getters for accessing internal state (useful for plugins)
    public JsonNode getGameData() { return gameData; }
    public JsonNode getState() { return state; }
    public ObjectMapper getObjectMapper() { return objectMapper; }
}
