package com.eventsourcing.gameSystem.context;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Complete context for a specific location, combining static adventure data
 * with dynamic world state. Used to provide rich environmental awareness
 * to AI systems and game logic.
 */
public record LocationContext(
    String locationId,
    String name,
    String description,
    String type,
    List<String> features,
    Map<String, String> connections,
    List<String> nearbyLocations,
    Map<String, String> secrets,
    List<String> currentOccupants,
    List<String> availableItems,
    boolean requiresLight,
    boolean hasBeenExplored,
    List<String> recentEvents,
    Map<String, Object> dynamicProperties,
    Instant lastModified
) {
    
    /**
     * Builder for constructing LocationContext instances
     */
    public static class Builder {
        private String locationId;
        private String name;
        private String description;
        private String type;
        private List<String> features = List.of();
        private Map<String, String> connections = Map.of();
        private List<String> nearbyLocations = List.of();
        private Map<String, String> secrets = Map.of();
        private List<String> currentOccupants = List.of();
        private List<String> availableItems = List.of();
        private boolean requiresLight = false;
        private boolean hasBeenExplored = false;
        private List<String> recentEvents = List.of();
        private Map<String, Object> dynamicProperties = Map.of();
        private Instant lastModified = Instant.now();
        
        public Builder locationId(String locationId) {
            this.locationId = locationId;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder type(String type) {
            this.type = type;
            return this;
        }
        
        public Builder features(List<String> features) {
            this.features = features != null ? features : List.of();
            return this;
        }
        
        public Builder connections(Map<String, String> connections) {
            this.connections = connections != null ? connections : Map.of();
            return this;
        }
        
        public Builder nearbyLocations(List<String> nearbyLocations) {
            this.nearbyLocations = nearbyLocations != null ? nearbyLocations : List.of();
            return this;
        }
        
        public Builder secrets(Map<String, String> secrets) {
            this.secrets = secrets != null ? secrets : Map.of();
            return this;
        }
        
        public Builder currentOccupants(List<String> currentOccupants) {
            this.currentOccupants = currentOccupants != null ? currentOccupants : List.of();
            return this;
        }
        
        public Builder availableItems(List<String> availableItems) {
            this.availableItems = availableItems != null ? availableItems : List.of();
            return this;
        }
        
        public Builder requiresLight(boolean requiresLight) {
            this.requiresLight = requiresLight;
            return this;
        }
        
        public Builder hasBeenExplored(boolean hasBeenExplored) {
            this.hasBeenExplored = hasBeenExplored;
            return this;
        }
        
        public Builder recentEvents(List<String> recentEvents) {
            this.recentEvents = recentEvents != null ? recentEvents : List.of();
            return this;
        }
        
        public Builder dynamicProperties(Map<String, Object> dynamicProperties) {
            this.dynamicProperties = dynamicProperties != null ? dynamicProperties : Map.of();
            return this;
        }
        
        public Builder lastModified(Instant lastModified) {
            this.lastModified = lastModified != null ? lastModified : Instant.now();
            return this;
        }
        
        public LocationContext build() {
            return new LocationContext(
                locationId, name, description, type, features, connections,
                nearbyLocations, secrets, currentOccupants, availableItems,
                requiresLight, hasBeenExplored, recentEvents, dynamicProperties,
                lastModified
            );
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Convenience methods for common queries
    
    public boolean hasSecrets() {
        return !secrets.isEmpty();
    }
    
    public boolean isEmpty() {
        return currentOccupants.isEmpty() && availableItems.isEmpty();
    }
    
    public boolean isAccessibleFrom(String otherLocationId) {
        return connections.containsValue(otherLocationId);
    }
    
    public Optional<String> getDirectionTo(String targetLocationId) {
        return connections.entrySet().stream()
            .filter(entry -> entry.getValue().equals(targetLocationId))
            .map(Map.Entry::getKey)
            .findFirst();
    }
    
    public List<String> getAccessibleFeatures() {
        // Filter features based on context (light requirements, etc.)
        if (requiresLight && !currentOccupants.contains("light_source")) {
            return features.stream()
                .filter(feature -> !feature.contains("hidden") && !feature.contains("examine"))
                .toList();
        }
        return features;
    }
    
    public String getSecretHints() {
        if (!hasSecrets()) return "";
        
        return secrets.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .reduce((a, b) -> a + "; " + b)
            .orElse("");
    }
    
    public boolean canPerformAction(String action) {
        return switch (action.toLowerCase()) {
            case "search", "examine" -> !isEmpty() || hasSecrets();
            case "move", "go" -> !connections.isEmpty();
            case "rest" -> type.equals("town") || features.contains("safe_haven");
            case "light" -> requiresLight;
            default -> true; // Allow most actions by default
        };
    }
}
