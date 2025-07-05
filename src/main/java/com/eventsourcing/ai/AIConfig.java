package com.eventsourcing.ai;

import java.util.Map;

/**
 * Configuration for AI services.
 * Loads settings from environment variables.
 */
public class AIConfig {
    
    private final String claudeApiKey;
    private final String claudeModel;
    private final int maxTokens;
    private final double temperature;
    private final int requestsPerMinute;
    private final int cacheTtlMinutes;
    
    public AIConfig() {
        this.claudeApiKey = getEnvOrDefault("CLAUDE_API_KEY", "");
        this.claudeModel = getEnvOrDefault("CLAUDE_MODEL", "claude-sonnet-4-20250514");
        this.maxTokens = Integer.parseInt(getEnvOrDefault("CLAUDE_MAX_TOKENS", "1000"));
        this.temperature = Double.parseDouble(getEnvOrDefault("CLAUDE_TEMPERATURE", "0.7"));
        this.requestsPerMinute = Integer.parseInt(getEnvOrDefault("AI_REQUESTS_PER_MINUTE", "60"));
        this.cacheTtlMinutes = Integer.parseInt(getEnvOrDefault("AI_CACHE_TTL_MINUTES", "30"));
        
        // Debug output
        System.out.println("🔧 AIConfig Debug:");
        System.out.println("  - CLAUDE_API_KEY from env: " + (System.getenv("CLAUDE_API_KEY") != null ? "SET" : "NOT SET"));
        System.out.println("  - CLAUDE_API_KEY from props: " + (System.getProperty("CLAUDE_API_KEY") != null ? "SET" : "NOT SET"));
        System.out.println("  - Final API key length: " + (claudeApiKey != null ? claudeApiKey.length() : 0));
        System.out.println("  - Is configured: " + isConfigured());
    }
    
    // For testing with custom values
    public AIConfig(String claudeApiKey, String claudeModel, int maxTokens, double temperature) {
        this.claudeApiKey = claudeApiKey;
        this.claudeModel = claudeModel;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.requestsPerMinute = 60;
        this.cacheTtlMinutes = 30;
    }
    
    public String getClaudeApiKey() { return claudeApiKey; }
    public String getClaudeModel() { return claudeModel; }
    public int getMaxTokens() { return maxTokens; }
    public double getTemperature() { return temperature; }
    public int getRequestsPerMinute() { return requestsPerMinute; }
    public int getCacheTtlMinutes() { return cacheTtlMinutes; }
    
    public boolean isConfigured() {
        return claudeApiKey != null && !claudeApiKey.isEmpty() && !claudeApiKey.equals("your_claude_api_key_here");
    }
    
    private String getEnvOrDefault(String key, String defaultValue) {
        // First try environment variable
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        
        // Then try system property (set by EnvLoader)
        value = System.getProperty(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        
        return defaultValue;
    }
    
    @Override
    public String toString() {
        return String.format("AIConfig{model='%s', maxTokens=%d, temperature=%.1f, configured=%s}", 
            claudeModel, maxTokens, temperature, isConfigured());
    }
}
