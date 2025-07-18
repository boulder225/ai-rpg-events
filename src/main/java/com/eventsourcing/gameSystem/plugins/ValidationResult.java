package com.eventsourcing.gameSystem.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of validating an action against game rules
 */
public class ValidationResult {
    private final boolean valid;
    private final List<String> violations;
    private final List<String> suggestions;
    private final String pluginName;
    
    public ValidationResult(boolean valid, List<String> violations, List<String> suggestions, String pluginName) {
        this.valid = valid;
        this.violations = violations != null ? new ArrayList<>(violations) : new ArrayList<>();
        this.suggestions = suggestions != null ? new ArrayList<>(suggestions) : new ArrayList<>();
        this.pluginName = pluginName;
    }
    
    public ValidationResult(boolean valid, String pluginName) {
        this(valid, new ArrayList<>(), new ArrayList<>(), pluginName);
    }
    
    public static ValidationResult valid(String pluginName) {
        return new ValidationResult(true, pluginName);
    }
    
    public static ValidationResult invalid(String violation, String pluginName) {
        List<String> violations = new ArrayList<>();
        violations.add(violation);
        return new ValidationResult(false, violations, new ArrayList<>(), pluginName);
    }
    
    public static ValidationResult invalid(List<String> violations, String pluginName) {
        return new ValidationResult(false, violations, new ArrayList<>(), pluginName);
    }
    
    public static ValidationResult invalidWithSuggestion(String violation, String suggestion, String pluginName) {
        List<String> violations = new ArrayList<>();
        violations.add(violation);
        List<String> suggestions = new ArrayList<>();
        suggestions.add(suggestion);
        return new ValidationResult(false, violations, suggestions, pluginName);
    }
    
    // Getters
    public boolean isValid() { 
        return valid; 
    }
    
    public List<String> getViolations() { 
        return Collections.unmodifiableList(violations); 
    }
    
    public List<String> getSuggestions() { 
        return Collections.unmodifiableList(suggestions); 
    }
    
    public String getPluginName() {
        return pluginName;
    }
    
    public boolean hasViolations() {
        return !violations.isEmpty();
    }
    
    public boolean hasSuggestions() {
        return !suggestions.isEmpty();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationResult{");
        sb.append("valid=").append(valid);
        sb.append(", plugin='").append(pluginName).append("'");
        if (!violations.isEmpty()) {
            sb.append(", violations=").append(violations);
        }
        if (!suggestions.isEmpty()) {
            sb.append(", suggestions=").append(suggestions);
        }
        sb.append('}');
        return sb.toString();
    }
}
