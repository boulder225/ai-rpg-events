package com.eventsourcing.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration for AI services.
 * Loads settings from environment variables.
 */
public record AIConfig(
    String claudeApiKey,
    String claudeModel,
    int maxTokens,
    double temperature,
    int requestsPerMinute,
    int cacheTtlMinutes
) {
    
    private static final Logger log = LoggerFactory.getLogger(AIConfig.class);
    
    // Compact constructor for validation
    public AIConfig {
        if (temperature < 0 || temperature > 1) {
            throw new IllegalArgumentException("Invalid temperature: " + temperature);
        }
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("Invalid maxTokens: " + maxTokens);
        }
        if (requestsPerMinute <= 0) {
            throw new IllegalArgumentException("Invalid requestsPerMinute: " + requestsPerMinute);
        }
        if (cacheTtlMinutes <= 0) {
            throw new IllegalArgumentException("Invalid cacheTtlMinutes: " + cacheTtlMinutes);
        }
    }
    
    /**
     * Factory method to create configuration from environment variables.
     */
    public static AIConfig fromEnvironment() {
        var config = new AIConfig(
            getEnvOrDefault("CLAUDE_API_KEY", ""),
            getEnvOrDefault("CLAUDE_MODEL", "claude-sonnet-4-20250514"),
            Integer.parseInt(getEnvOrDefault("CLAUDE_MAX_TOKENS", "1000")),
            Double.parseDouble(getEnvOrDefault("CLAUDE_TEMPERATURE", "0.7")),
            Integer.parseInt(getEnvOrDefault("AI_REQUESTS_PER_MINUTE", "60")),
            Integer.parseInt(getEnvOrDefault("AI_CACHE_TTL_MINUTES", "30"))
        );
        
        // Structured logging for AI configuration
        log.info("🔧 AIConfig initialized: model={}, configured={}, maxTokens={}, temperature={}", 
            config.claudeModel(), 
            config.isConfigured(),
            config.maxTokens(),
            config.temperature());
            
        log.debug("Environment configuration: apiKeyFromEnv={}, apiKeyFromProps={}, finalKeyLength={}", 
            System.getenv("CLAUDE_API_KEY") != null ? "SET" : "NOT_SET",
            System.getProperty("CLAUDE_API_KEY") != null ? "SET" : "NOT_SET",
            config.claudeApiKey() != null ? config.claudeApiKey().length() : 0);
        
        return config;
    }
    
    /**
     * Factory method for testing with custom values.
     */
    public static AIConfig forTesting(String claudeApiKey, String claudeModel, int maxTokens, double temperature) {
        return new AIConfig(claudeApiKey, claudeModel, maxTokens, temperature, 60, 30);
    }
    
    /**
     * Check if the configuration is valid for use.
     */
    public boolean isConfigured() {
        return claudeApiKey != null && !claudeApiKey.isEmpty() 
            && !claudeApiKey.equals("your_claude_api_key_here");
    }
    
    private static String getEnvOrDefault(String key, String defaultValue) {
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
}
