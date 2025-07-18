package com.eventsourcing.gameSystem.context;

import com.eventsourcing.gameSystem.core.AdventureData;
import com.eventsourcing.gameSystem.core.LocationData;
import com.eventsourcing.rpg.RPGCommandHandler;
import com.eventsourcing.rpg.RPGEvent;
import com.eventsourcing.rpg.RPGState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Centralized service for managing location context in RPG games.
 * Combines static adventure data with dynamic world state to provide
 * complete environmental awareness for AI and game systems.
 */
public class LocationContextManager {
    
    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(LocationContextManager.class.getName());
    private static final Logger LOGGER = Logger.getLogger(LocationContextManager.class.getName());
    
    private final AdventureData adventureData;
    private final RPGCommandHandler commandHandler;
    private final ObjectMapper objectMapper;
    
    // Enhanced cache with simple metrics
    private final Map<String, LocationContext> contextCache = new ConcurrentHashMap<>();
    private final Map<String, Instant> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 30_000; // 30 seconds
    
    // Simple cache metrics
    private long cacheHits = 0;
    private long cacheMisses = 0;
    private long cacheCleanups = 0;
    
    public LocationContextManager(AdventureData adventureData, RPGCommandHandler commandHandler) {
        this.adventureData = adventureData;
        this.commandHandler = commandHandler;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Get complete context for a specific location and player
     */
    public LocationContext getFullLocationContext(String locationId, String playerId) {
        // Normalize location ID (handle case sensitivity and whitespace)
        String normalizedLocationId = normalizeLocationId(locationId);
        
        // Check cache first with automatic cleanup
        String cacheKey = normalizedLocationId + ":" + playerId;
        LocationContext cached = getCachedContextWithCleanup(cacheKey);
        if (cached != null) {
            cacheHits++;
            return cached;
        }
        
        cacheMisses++;
        
        try {
            // Get static location data from adventure
            LocationData locationData = getLocationData(normalizedLocationId);
            if (locationData == null) {
                LOGGER.warning("Location not found in adventure data: " + normalizedLocationId);
                return createFallbackContext(normalizedLocationId);
            }
            
            // Get dynamic state from game handler
            RPGState.LocationState locationState = getLocationState(normalizedLocationId);
            RPGState.PlayerState playerState = commandHandler.getPlayerState(playerId);
            
            // Build comprehensive context
            LocationContext context = buildLocationContext(locationData, locationState, playerState);
            
            // Cache the result
            cacheContext(cacheKey, context);
            
            return context;
            
        } catch (Exception e) {
            LOGGER.severe("Failed to build location context for " + locationId + ": " + e.getMessage());
            return createFallbackContext(normalizedLocationId);
        }
    }
    
    /**
     * Handle location change events to refresh context and trigger updates
     */
    public void onPlayerMovement(RPGEvent.PlayerMovedToLocation event) {
        String playerId = event.playerId();
        String newLocationId = event.toLocationId();
        String oldLocationId = event.fromLocationId();
        
        LOGGER.info(String.format("\uD83D\uDCCD Location Context: Player %s moved from %s to %s", playerId, oldLocationId, newLocationId));
        
        // Invalidate relevant cache entries
        invalidatePlayerCache(playerId);
        
        // Get fresh context for new location
        LocationContext newContext = getFullLocationContext(newLocationId, playerId);
        
        LOGGER.info(String.format("\u2728 Context refreshed for %s: %s (%s)", 
            playerId, newContext.name(), newContext.type()));
        
        // Log movement with context
        logLocationMovement(event, newContext);
        
        // Trigger any location-specific events or notifications
        handleLocationEvents(newContext, playerId);
    }
    
    /**
     * Get enhanced AI context prompt with full location details
     */
    public String generateEnhancedAIPrompt(RPGState.PlayerState playerState, String basePrompt) {
        String locationId = playerState.currentLocationId();
        log.info(String.format("[GENERATE ENHANCED AI PROMPT] Location ID: %s", locationId));
        if (locationId == null || locationId.isEmpty()) {
            locationId = "village"; // Default fallback
        }
        
        LocationContext locationContext = getFullLocationContext(locationId, playerState.playerId());
        
        StringBuilder enhancedPrompt = new StringBuilder();
        
        // Location-specific context
        enhancedPrompt.append("=== CURRENT LOCATION CONTEXT ===\n");
        enhancedPrompt.append("Location: ").append(locationContext.name()).append("\n");
        enhancedPrompt.append("Type: ").append(locationContext.type()).append("\n");
        enhancedPrompt.append("Description: ").append(locationContext.description()).append("\n\n");
        
        // Environmental features
        if (!locationContext.features().isEmpty()) {
            enhancedPrompt.append("Available Features:\n");
            locationContext.getAccessibleFeatures().forEach(feature -> 
                enhancedPrompt.append("- ").append(feature).append("\n"));
            enhancedPrompt.append("\n");
        }
        
        // Exit options
        if (!locationContext.connections().isEmpty()) {
            enhancedPrompt.append("Available Exits:\n");
            locationContext.connections().forEach((direction, targetId) -> {
                LocationData target = getLocationData(targetId);
                String targetName = target != null ? target.name() : targetId;
                enhancedPrompt.append("- ").append(direction).append(" to ")
                           .append(targetName).append("\n");
            });
            enhancedPrompt.append("\n");
        }
        
        // Current occupants and items
        if (!locationContext.currentOccupants().isEmpty()) {
            enhancedPrompt.append("Others Present: ");
            enhancedPrompt.append(String.join(", ", locationContext.currentOccupants()));
            enhancedPrompt.append("\n\n");
        }
        
        if (!locationContext.availableItems().isEmpty()) {
            enhancedPrompt.append("Items Visible: ");
            enhancedPrompt.append(String.join(", ", locationContext.availableItems()));
            enhancedPrompt.append("\n\n");
        }
        
        // Environmental conditions
        if (locationContext.requiresLight()) {
            enhancedPrompt.append("LIGHTING: This area is dark and requires a light source.\n");
        }
        
        // Hidden knowledge (only if player has discovered)
        if (locationContext.hasBeenExplored() && locationContext.hasSecrets()) {
            enhancedPrompt.append("Known Secrets: ").append(locationContext.getSecretHints()).append("\n");
        }
        
        // Recent events in this location
        if (!locationContext.recentEvents().isEmpty()) {
            enhancedPrompt.append("Recent Activity:\n");
            locationContext.recentEvents().forEach(event -> 
                enhancedPrompt.append("- ").append(event).append("\n"));
            enhancedPrompt.append("\n");
        }
        
        // Append the original prompt
        enhancedPrompt.append(basePrompt);
        
        return enhancedPrompt.toString();
    }
    
    /**
     * Validate if an action is possible in the current location context
     */
    public boolean validateLocationAction(String action, LocationContext context) {
        return context.canPerformAction(action);
    }
    
    // Private helper methods
    
    private String normalizeLocationId(String locationId) {
        if (locationId == null) return "village";
        return locationId.trim().toLowerCase();
    }
    
    private LocationData getLocationData(String locationId) {
        return adventureData.locations().values().stream()
            .filter(loc -> locationId.equals(loc.id().toLowerCase()))
            .findFirst()
            .orElse(null);
    }
    
    private RPGState.LocationState getLocationState(String locationId) {
        var state = commandHandler.getLocationState(locationId);
        if (state == null) {
            // Create and store a default LocationState if missing
            state = new RPGState.LocationState(
                locationId, "unknown", Map.of(), List.of(),
                Map.of(), List.of(), List.of(), Instant.now()
            );
            commandHandler.putLocationState(locationId, state);
        }
        return state;
    }
    
    private LocationContext buildLocationContext(LocationData locationData, 
                                               RPGState.LocationState locationState, 
                                               RPGState.PlayerState playerState) {
        
        // Determine if location requires light
        boolean requiresLight = locationData.type().equals("dungeon") && 
            !locationData.features().contains("natural_light");
        
        // Check if player has been here before
        boolean hasBeenExplored = playerState.actionHistory().stream()
            .anyMatch(action -> locationData.id().equals(action.locationId()));
        
        // Build nearby locations list
        List<String> nearbyLocations = locationData.connections().stream()
            .map(this::getLocationData)
            .filter(Objects::nonNull)
            .map(LocationData::name)
            .collect(Collectors.toList());
        
        // Convert connections to direction->locationId map
        Map<String, String> connectionMap = new HashMap<>();
        for (String connectedId : locationData.connections()) {
            // Determine direction based on location relationships
            String direction = determineDirection(locationData.id(), connectedId);
            connectionMap.put(direction, connectedId);
        }
        
        return LocationContext.builder()
            .locationId(locationData.id())
            .name(locationData.name())
            .description(locationData.description())
            .type(locationData.type())
            .features(locationData.features())
            .connections(connectionMap)
            .nearbyLocations(nearbyLocations)
            .secrets(locationData.secrets())
            .currentOccupants(locationState.currentOccupants())
            .availableItems(locationState.itemsFound())
            .requiresLight(requiresLight)
            .hasBeenExplored(hasBeenExplored)
            .recentEvents(getRecentLocationEvents(locationState))
            .dynamicProperties(Map.of(
                "visitedBy", locationState.visitedBy().size(),
                "lastModified", locationState.lastModified().toString()
            ))
            .lastModified(locationState.lastModified())
            .build();
    }
    
    private String determineDirection(String fromId, String toId) {
        // Simple heuristic based on location names/IDs
        // In a full implementation, this would use spatial data
        if (toId.contains("north") || toId.compareTo(fromId) < 0) return "north";
        if (toId.contains("south") || toId.compareTo(fromId) > 0) return "south";
        if (toId.contains("east")) return "east";
        if (toId.contains("west")) return "west";
        if (toId.contains("entrance") || toId.equals("village")) return "outside";
        return "passage";
    }
    
    private List<String> getRecentLocationEvents(RPGState.LocationState locationState) {
        // Extract recent meaningful events from location properties
        List<String> events = new ArrayList<>();
        String lastChange = locationState.properties().get("lastChange");
        if (lastChange != null) {
            events.add(lastChange);
        }
        return events;
    }
    
    private LocationContext createFallbackContext(String locationId) {
        return LocationContext.builder()
            .locationId(locationId)
            .name("Unknown Location")
            .description("You find yourself in an unfamiliar place.")
            .type("unknown")
            .features(List.of())
            .connections(Map.of())
            .build();
    }
    
    private LocationContext getCachedContextWithCleanup(String cacheKey) {
        Instant cacheTime = cacheTimestamps.get(cacheKey);
        if (cacheTime != null) {
            if (Instant.now().minusMillis(CACHE_DURATION_MS).isBefore(cacheTime)) {
                return contextCache.get(cacheKey);
            } else {
                // Automatic cleanup of expired entry
                contextCache.remove(cacheKey);
                cacheTimestamps.remove(cacheKey);
                cacheCleanups++;
            }
        }
        return null;
    }
    
    private void cacheContext(String cacheKey, LocationContext context) {
        contextCache.put(cacheKey, context);
        cacheTimestamps.put(cacheKey, Instant.now());
    }
    
    /**
     * Get simple cache statistics
     */
    public Map<String, Object> getCacheStats() {
        long totalRequests = cacheHits + cacheMisses;
        double hitRatio = totalRequests > 0 ? (double) cacheHits / totalRequests : 0.0;
        
        return Map.of(
            "cache_size", contextCache.size(),
            "cache_hits", cacheHits,
            "cache_misses", cacheMisses,
            "hit_ratio", Math.round(hitRatio * 100.0) / 100.0, // Round to 2 decimals
            "cleanups_performed", cacheCleanups,
            "cache_duration_seconds", CACHE_DURATION_MS / 1000
        );
    }
    
    /**
     * Get current cache size for monitoring
     */
    public int getCacheSize() {
        return contextCache.size();
    }
    
    /**
     * Periodic cleanup of expired entries (call from scheduled task if needed)
     */
    public void cleanupExpiredEntries() {
        Instant now = Instant.now();
        int removedCount = 0;
        
        var iterator = cacheTimestamps.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now.minusMillis(CACHE_DURATION_MS).isAfter(entry.getValue())) {
                contextCache.remove(entry.getKey());
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            cacheCleanups += removedCount;
            LOGGER.info("Cleaned up {} expired cache entries", removedCount);
        }
    }
    
    private void invalidatePlayerCache(String playerId) {
        // Remove all cache entries for this player
        contextCache.entrySet().removeIf(entry -> 
            entry.getKey().endsWith(":" + playerId));
        cacheTimestamps.entrySet().removeIf(entry -> 
            entry.getKey().endsWith(":" + playerId));
    }
    
    private void logLocationMovement(RPGEvent.PlayerMovedToLocation event, LocationContext context) {
        LOGGER.info(String.format(
            "Location change: Player %s entered %s (%s) - Features: %s", 
            event.playerId(), 
            context.name(), 
            context.type(),
            context.features().size()
        ));
    }
    
    private void handleLocationEvents(LocationContext context, String playerId) {
        // Trigger any location-specific events or mechanics
        // This could include:
        // - Random encounters
        // - Environmental effects  
        // - Discovery opportunities
        // - Context-sensitive hints
        
        if (context.type().equals("dungeon") && context.requiresLight()) {
            LOGGER.info("Player entered dark area - light source required");
        }
        
        if (context.hasSecrets() && !context.hasBeenExplored()) {
            LOGGER.info("Location has undiscovered secrets");
        }
    }
    
    // Public utility methods
    
    public void clearCache() {
        contextCache.clear();
        cacheTimestamps.clear();
    }
    
    public int getCacheSize() {
        return contextCache.size();
    }
    
    public Map<String, String> getCacheStats() {
        return Map.of(
            "cached_contexts", String.valueOf(contextCache.size()),
            "cache_duration_ms", String.valueOf(CACHE_DURATION_MS)
        );
    }
}
