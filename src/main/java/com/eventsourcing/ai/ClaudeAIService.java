package com.eventsourcing.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eventsourcing.logging.RPGLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Claude AI integration for autonomous game master responses.
 * Provides intelligent, contextual AI responses for RPG interactions.
 */
public class ClaudeAIService {
    
    private static final Logger log = LoggerFactory.getLogger(ClaudeAIService.class);
    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    
    private final AIConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;
    private final AICache cache;
    private final AIMetrics metrics;
    
    public ClaudeAIService(AIConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.objectMapper = new ObjectMapper();
        this.rateLimiter = new RateLimiter(config.requestsPerMinute());
        this.cache = new AICache(config.cacheTtlMinutes());
        this.metrics = new AIMetrics();
    }
    
    /**
     * Generate an AI Game Master response based on game context and player action.
     */
    public AIResponse generateGameMasterResponse(String context, String playerAction) {
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        RPGLogger.setAIContext(config.claudeModel(), requestId);
        String primaryModel = config.claudeModel();
        String fallbackModel = "claude-3-opus-20240229";
        try {
            if (!config.isConfigured()) {
                log.warn("AI service not configured, returning fallback response (model: none)");
                return AIResponse.fallback("AI not configured", "not_configured", "none");
            }
            var cacheKey = generateCacheKey(context, playerAction);
            var cachedResponse = cache.get(cacheKey);
            if (cachedResponse != null) {
                log.debug("Cache hit for GM response: requestId={}, model={}", requestId, primaryModel);
                metrics.recordCacheHit();
                return cachedResponse;
            }
            if (!rateLimiter.tryAcquire()) {
                log.warn("Rate limit exceeded for GM response: requestId={}, model={}", requestId, primaryModel);
                metrics.recordRateLimit();
                return AIResponse.fallback("Rate limit exceeded", "rate_limit", primaryModel);
            }
            try {
                var prompt = buildGameMasterPrompt(context, playerAction);
                log.info("Calling Claude API for GM response: requestId={}, promptLength={}, model={}", requestId, prompt.length(), primaryModel);
                var response = callClaudeAPI(prompt, primaryModel);
                switch (response) {
                    case AIResponse.Success success -> {
                        log.info("GM response successful: requestId={}, tokens={}, contentLength={}, model={}", requestId, success.getTotalTokens(), success.content().length(), primaryModel);
                        metrics.recordRequest(true);
                        cache.put(cacheKey, response);
                        return response;
                    }
                    case AIResponse.Error error -> {
                        log.error("GM response failed: requestId={}, error={}, model={}", requestId, error.errorMessage(), primaryModel);
                        metrics.recordRequest(false);
                        // Check for overload and try fallback model
                        if (error.errorMessage() != null && (error.errorMessage().toLowerCase().contains("overloaded") || error.errorMessage().contains("529"))) {
                            log.warn("Primary model overloaded, retrying with fallback model: {} (requestId={})", fallbackModel, requestId);
                            try {
                                var fallbackResponse = callClaudeAPI(prompt, fallbackModel);
                                switch (fallbackResponse) {
                                    case AIResponse.Success success2 -> {
                                        log.info("Fallback model response successful: requestId={}, model={}", requestId, fallbackModel);
                                        return success2;
                                    }
                                    case AIResponse.Fallback fallback2 -> {
                                        log.warn("Fallback model returned fallback: requestId={}, model={}", requestId, fallbackModel);
                                        return fallback2;
                                    }
                                    case AIResponse.Error error2 -> {
                                        log.error("Fallback model also failed: requestId={}, model={}, error={}", requestId, fallbackModel, error2.errorMessage());
                                        return AIResponse.fallback("Both primary and fallback Claude models overloaded.", "overloaded", fallbackModel);
                                    }
                                }
                            } catch (Exception ex) {
                                log.error("Exception during fallback model call: requestId={}, model={}, error={}", requestId, fallbackModel, ex.getMessage(), ex);
                                return AIResponse.fallback("Claude fallback model error", "fallback_error", fallbackModel);
                            }
                        }
                        return AIResponse.fallback("Claude API error", "api_error", primaryModel);
                    }
                    case AIResponse.Fallback fallback -> {
                        log.warn("Claude API returned fallback: requestId={}, model={}", requestId, primaryModel);
                        return fallback;
                    }
                }
            } catch (Exception e) {
                log.error("Exception during Claude API call: requestId={}, model={}, error={}", requestId, primaryModel, e.getMessage(), e);
                return AIResponse.fallback("Claude API exception", "exception", primaryModel);
            }
        } catch (Exception e) {
            log.error("Unexpected error in generateGameMasterResponse: requestId={}, error={}", requestId, e.getMessage(), e);
            return AIResponse.fallback("Unexpected error", "unexpected_error", "none");
        }
    }
    
    /**
     * Generate NPC dialogue based on character personality and context.
     */
    public AIResponse generateNPCDialogue(String npcName, String personality, String context, String playerInput) {
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        String model = config.claudeModel();
        if (!config.isConfigured()) {
            log.warn("AI service not configured for NPC dialogue: npc={}, model=none", npcName);
            return AIResponse.fallback("AI not configured for NPC dialogue", "not_configured", "none");
        }
        try {
            var prompt = buildNPCPrompt(npcName, personality, context, playerInput);
            log.info("Calling Claude API for NPC dialogue: requestId={}, npc={}, model={}", requestId, npcName, model);
            var response = callClaudeAPI(prompt, model);
            switch (response) {
                case AIResponse.Success success -> {
                    log.info("NPC dialogue successful: requestId={}, npc={}, model={}", requestId, npcName, model);
                    return success;
                }
                case AIResponse.Fallback fallback -> {
                    log.warn("Claude API returned fallback for NPC: requestId={}, npc={}, model={}", requestId, npcName, model);
                    return fallback;
                }
                case AIResponse.Error error -> {
                    log.error("Claude API error for NPC: requestId={}, npc={}, model={}, error={}", requestId, npcName, model, error.errorMessage());
                    return AIResponse.fallback("Claude API error for NPC", "api_error", model);
                }
            }
        } catch (Exception e) {
            log.error("Exception during Claude API call for NPC: npc={}, model={}, error={}", npcName, model, e.getMessage(), e);
            return AIResponse.fallback("Claude API exception for NPC", "exception", model);
        }
    }
    
    /**
     * Generate world event descriptions for autonomous events.
     */
    public AIResponse generateWorldEvent(String eventType, String context) {
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        String model = config.claudeModel();
        if (!config.isConfigured()) {
            log.warn("AI service not configured for world event: eventType={}, model=none", eventType);
            return AIResponse.fallback("AI not configured for world event", "not_configured", "none");
        }
        try {
            var prompt = buildWorldEventPrompt(eventType, context);
            log.info("Calling Claude API for world event: requestId={}, eventType={}, model={}", requestId, eventType, model);
            var response = callClaudeAPI(prompt, model);
            switch (response) {
                case AIResponse.Success success -> {
                    log.info("World event successful: requestId={}, eventType={}, model={}", requestId, eventType, model);
                    return success;
                }
                case AIResponse.Fallback fallback -> {
                    log.warn("Claude API returned fallback for world event: requestId={}, eventType={}, model={}", requestId, eventType, model);
                    return fallback;
                }
                case AIResponse.Error error -> {
                    log.error("Claude API error for world event: requestId={}, eventType={}, model={}, error={}", requestId, eventType, model, error.errorMessage());
                    return AIResponse.fallback("Claude API error for world event", "api_error", model);
                }
            }
        } catch (Exception e) {
            log.error("Exception during Claude API call for world event: eventType={}, model={}, error={}", eventType, model, e.getMessage(), e);
            return AIResponse.fallback("Claude API exception for world event", "exception", model);
        }
    }
    
    private AIResponse callClaudeAPI(String prompt, String model) throws IOException, InterruptedException {
        var requestBody = objectMapper.writeValueAsString(Map.of(
            "model", model,
            "max_tokens", config.maxTokens(),
            "temperature", config.temperature(),
            "messages", java.util.List.of(
                Map.of(
                    "role", "user",
                    "content", prompt
                )
            )
        ));
        
        var request = HttpRequest.newBuilder()
            .uri(URI.create(CLAUDE_API_URL))
            .header("Content-Type", "application/json")
            .header("x-api-key", config.claudeApiKey())
            .header("anthropic-version", "2023-06-01")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Claude API error: " + response.statusCode() + " - " + response.body());
        }
        
        var jsonResponse = objectMapper.readTree(response.body());
        var content = jsonResponse.path("content").get(0).path("text").asText();
        var usage = jsonResponse.path("usage");
        
        return AIResponse.success(
            content,
            usage.path("input_tokens").asInt(),
            usage.path("output_tokens").asInt(),
            model
        );
    }
    
    private String buildGameMasterPrompt(String context, String playerAction) {
        return String.format("""
            Sei un esperto Game Master AI per un RPG fantasy immersivo. Il tuo ruolo è creare risposte 
            coinvolgenti e contestuali che danno vita al mondo.
            
            CONTESTO DI GIOCO:
            %s
            
            AZIONE DEL GIOCATORE: %s
            
            ISTRUZIONI:
            - Fornisci una risposta vivida e immersiva che riconosca l'azione del giocatore
            - Mantieni le risposte concise ma evocative (2-4 frasi)
            - Includi dettagli sensoriali (viste, suoni, atmosfera)
            - Considera la storia del giocatore e le relazioni nella tua risposta
            - Avanza la narrazione dando al giocatore scelte significative
            - Usa un tono narrativo coinvolgente ma non troppo drammatico
            - Rispondi SEMPRE in italiano
            - Usa un linguaggio ricco e descrittivo tipico della narrativa fantasy italiana
            
            Rispondi come il Game Master descrivendo cosa succede:
            """, context, playerAction);
    }
    
    private String buildNPCPrompt(String npcName, String personality, String context, String playerInput) {
        return String.format("""
            Sei %s, un NPC in un mondo RPG fantasy. 
            
            IL TUO PERSONAGGIO:
            - Nome: %s
            - Personalità: %s
            
            CONTESTO:
            %s
            
            IL GIOCATORE DICE: "%s"
            
            ISTRUZIONI:
            - Rispondi nel personaggio come %s
            - Mantieni la tua risposta conversazionale e naturale (1-3 frasi)
            - Mostra la tua personalità attraverso le tue parole e reazioni
            - Considera la tua relazione con il giocatore
            - Sii utile ma rimani fedele al tuo personaggio
            - Rispondi SEMPRE in italiano
            - Usa un linguaggio appropriato al tuo personaggio e al contesto fantasy
            
            Rispondi come farebbe %s:
            """, npcName, npcName, personality, context, playerInput, npcName, npcName);
    }
    
    private String buildWorldEventPrompt(String eventType, String context) {
        return String.format("""
            Stai descrivendo un evento autonomo del mondo in un RPG fantasy.
            
            TIPO DI EVENTO: %s
            CONTESTO DEL MONDO: %s
            
            ISTRUZIONI:
            - Descrivi questo evento in 2-3 frasi vivide
            - Fallo sembrare un mondo vivo e pulsante
            - Includi dettagli atmosferici
            - Considera come questo influenza la narrazione in corso
            - Rispondi SEMPRE in italiano
            - Usa un linguaggio evocativo e immersivo
            
            Descrivi l'evento:
            """, eventType, context);
    }
    
    private AIResponse createFallbackResponse(String playerAction) {
        return createFallbackResponse(playerAction, "AI service not configured");
    }
    
    private AIResponse createFallbackResponse(String playerAction, String reason) {
        var fallbackText = switch (playerAction.toLowerCase()) {
            case "/look around", "/look" -> 
                "🏰 Osservi i dintorni, assorbendo l'atmosfera mistica di questo regno incantato.";
            case "/attack goblin" -> 
                "⚔️ Ti impegni in combattimento con la creatura, la tua arma luccica nella luce fioca!";
            case "/talk tavern_keeper" -> 
                "🍺 Il taverniere annuisce in segno di riconoscimento, pronto a condividere saggezza locale e racconti.";
            default -> 
                "✨ Il mondo risponde alla tua azione con magia antica, anche se i dettagli rimangono misteriosi.";
        };
        
        return AIResponse.fallback(fallbackText, reason, config.claudeModel());
    }
    
    private AIResponse createFallbackNPCResponse(String npcName, String playerInput) {
        var fallbackText = String.format("💬 %s considera le tue parole pensierosamente, anche se la sua risposta sembra distante oggi.", npcName);
        
        return AIResponse.fallback(fallbackText, "AI service not available", config.claudeModel());
    }
    
    private AIResponse createFallbackWorldEvent(String eventType) {
        var fallbackText = String.format("🌍 Un %s si verifica nel mondo, i suoi effetti si propagano attraverso il regno.", eventType);
        
        return AIResponse.fallback(fallbackText, "AI service not available", config.claudeModel());
    }
    
    private String generateCacheKey(String... parts) {
        return String.join("|", parts).hashCode() + "";
    }
    
    public AIMetrics getMetrics() {
        return metrics;
    }
    
    public boolean isConfigured() {
        return config.isConfigured();
    }

    // Stub for compatibility with GenericGameContextManager
    public String generateResponse(String prompt) {
        return "[AI response stub for prompt: " + prompt + "]";
    }
}