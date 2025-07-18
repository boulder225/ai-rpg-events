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
        
        try {
            if (!config.isConfigured()) {
                log.warn("AI service not configured, returning fallback response");
                return createFallbackResponse(playerAction);
            }
            
            var cacheKey = generateCacheKey(context, playerAction);
            var cachedResponse = cache.get(cacheKey);
            if (cachedResponse != null) {
                log.debug("Cache hit for GM response: requestId={}", requestId);
                metrics.recordCacheHit();
                return cachedResponse;
            }
            
            if (!rateLimiter.tryAcquire()) {
                log.warn("Rate limit exceeded for GM response: requestId={}", requestId);
                metrics.recordRateLimit();
                return createFallbackResponse(playerAction, "Rate limit exceeded");
            }
            
            try {
                var prompt = buildGameMasterPrompt(context, playerAction);
                log.info("Calling Claude API for GM response: requestId={}, promptLength={}", requestId, prompt.length());
                var response = callClaudeAPI(prompt);
                
                switch (response) {
                    case AIResponse.Success success -> {
                        log.info("GM response successful: requestId={}, tokens={}, contentLength={}", 
                            requestId, success.getTotalTokens(), success.content().length());
                        metrics.recordRequest(true);
                        cache.put(cacheKey, response);
                        return response;
                    }
                    case AIResponse.Error error -> {
                        log.error("GM response failed: requestId={}, error={}", requestId, error.errorMessage());
                        metrics.recordRequest(false);
                        return createFallbackResponse(playerAction, error.errorMessage());
                    }
                    default -> {
                        log.warn("Unexpected GM response type: requestId={}, type={}", requestId, response.getClass().getSimpleName());
                        return response;
                    }
                }
                
            } catch (Exception e) {
                log.error("GM API call failed: requestId={}", requestId, e);
                metrics.recordRequest(false);
                return createFallbackResponse(playerAction, "AI service temporarily unavailable: " + e.getMessage());
            }
            
        } finally {
            RPGLogger.clearAIContext();
        }
    }
    
    /**
     * Generate NPC dialogue based on character personality and context.
     */
    public AIResponse generateNPCDialogue(String npcName, String personality, String context, String playerInput) {
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        RPGLogger.setAIContext(config.claudeModel(), requestId);
        
        try {
            if (!config.isConfigured()) {
                log.warn("AI service not configured for NPC dialogue: npc={}", npcName);
                return createFallbackNPCResponse(npcName, playerInput);
            }
            
            var cacheKey = generateCacheKey(npcName + personality, context + playerInput);
            var cachedResponse = cache.get(cacheKey);
            if (cachedResponse != null) {
                log.debug("Cache hit for NPC dialogue: requestId={}, npc={}", requestId, npcName);
                metrics.recordCacheHit();
                return cachedResponse;
            }
            
            if (!rateLimiter.tryAcquire()) {
                log.warn("Rate limit exceeded for NPC dialogue: requestId={}, npc={}", requestId, npcName);
                metrics.recordRateLimit();
                return createFallbackNPCResponse(npcName, playerInput);
            }
            
            try {
                var prompt = buildNPCPrompt(npcName, personality, context, playerInput);
                log.info("Calling Claude API for NPC dialogue: requestId={}, npc={}", requestId, npcName);
                var response = callClaudeAPI(prompt);
                
                switch (response) {
                    case AIResponse.Success success -> {
                        log.info("NPC dialogue successful: requestId={}, npc={}, tokens={}", 
                            requestId, npcName, success.getTotalTokens());
                        metrics.recordRequest(true);
                        cache.put(cacheKey, response);
                        return response;
                    }
                    case AIResponse.Error error -> {
                        log.error("NPC dialogue failed: requestId={}, npc={}, error={}", requestId, npcName, error.errorMessage());
                        metrics.recordRequest(false);
                        return createFallbackNPCResponse(npcName, playerInput);
                    }
                    default -> {
                        return response;
                    }
                }
                
            } catch (Exception e) {
                log.error("NPC API call failed: requestId={}, npc={}", requestId, npcName, e);
                metrics.recordRequest(false);
                return createFallbackNPCResponse(npcName, playerInput);
            }
            
        } finally {
            RPGLogger.clearAIContext();
        }
    }
    
    /**
     * Generate world event descriptions for autonomous events.
     */
    public AIResponse generateWorldEvent(String eventType, String context) {
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        RPGLogger.setAIContext(config.claudeModel(), requestId);
        
        try {
            if (!config.isConfigured()) {
                log.warn("AI service not configured for world event: eventType={}", eventType);
                return createFallbackWorldEvent(eventType);
            }
            
            try {
                var prompt = buildWorldEventPrompt(eventType, context);
                log.info("Calling Claude API for world event: requestId={}, eventType={}", requestId, eventType);
                var response = callClaudeAPI(prompt);
                
                switch (response) {
                    case AIResponse.Success success -> {
                        log.info("World event successful: requestId={}, eventType={}, tokens={}", 
                            requestId, eventType, success.getTotalTokens());
                        metrics.recordRequest(true);
                        return response;
                    }
                    case AIResponse.Error error -> {
                        log.error("World event failed: requestId={}, eventType={}, error={}", requestId, eventType, error.errorMessage());
                        metrics.recordRequest(false);
                        return createFallbackWorldEvent(eventType);
                    }
                    default -> {
                        return response;
                    }
                }
                
            } catch (Exception e) {
                log.error("World event API call failed: requestId={}, eventType={}", requestId, eventType, e);
                metrics.recordRequest(false);
                return createFallbackWorldEvent(eventType);
            }
            
        } finally {
            RPGLogger.clearAIContext();
        }
    }
    
    private AIResponse callClaudeAPI(String prompt) throws IOException, InterruptedException {
        var requestBody = objectMapper.writeValueAsString(Map.of(
            "model", config.claudeModel(),
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
            usage.path("output_tokens").asInt()
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
        
        return AIResponse.fallback(fallbackText, reason);
    }
    
    private AIResponse createFallbackNPCResponse(String npcName, String playerInput) {
        var fallbackText = String.format("💬 %s considera le tue parole pensierosamente, anche se la sua risposta sembra distante oggi.", npcName);
        
        return AIResponse.fallback(fallbackText, "AI service not available");
    }
    
    private AIResponse createFallbackWorldEvent(String eventType) {
        var fallbackText = String.format("🌍 Un %s si verifica nel mondo, i suoi effetti si propagano attraverso il regno.", eventType);
        
        return AIResponse.fallback(fallbackText, "AI service not available");
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