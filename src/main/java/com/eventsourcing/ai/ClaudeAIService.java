package com.eventsourcing.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        this.rateLimiter = new RateLimiter(config.getRequestsPerMinute());
        this.cache = new AICache(config.getCacheTtlMinutes());
        this.metrics = new AIMetrics();
    }
    
    /**
     * Generate an AI Game Master response based on game context and player action.
     */
    public AIResponse generateGameMasterResponse(String context, String playerAction) {
        if (!config.isConfigured()) {
            return createFallbackResponse(playerAction);
        }
        
        var cacheKey = generateCacheKey(context, playerAction);
        var cachedResponse = cache.get(cacheKey);
        if (cachedResponse != null) {
            metrics.recordCacheHit();
            return cachedResponse;
        }
        
        if (!rateLimiter.tryAcquire()) {
            metrics.recordRateLimit();
            return createFallbackResponse(playerAction, "Rate limit exceeded");
        }
        
        try {
            var prompt = buildGameMasterPrompt(context, playerAction);
            var response = callClaudeAPI(prompt);
            
            metrics.recordRequest(true);
            cache.put(cacheKey, response);
            return response;
            
        } catch (Exception e) {
            metrics.recordRequest(false);
            return createFallbackResponse(playerAction, "AI service temporarily unavailable: " + e.getMessage());
        }
    }
    
    /**
     * Generate NPC dialogue based on character personality and context.
     */
    public AIResponse generateNPCDialogue(String npcName, String personality, String context, String playerInput) {
        if (!config.isConfigured()) {
            return createFallbackNPCResponse(npcName, playerInput);
        }
        
        var cacheKey = generateCacheKey(npcName + personality, context + playerInput);
        var cachedResponse = cache.get(cacheKey);
        if (cachedResponse != null) {
            metrics.recordCacheHit();
            return cachedResponse;
        }
        
        if (!rateLimiter.tryAcquire()) {
            metrics.recordRateLimit();
            return createFallbackNPCResponse(npcName, playerInput);
        }
        
        try {
            var prompt = buildNPCPrompt(npcName, personality, context, playerInput);
            var response = callClaudeAPI(prompt);
            
            metrics.recordRequest(true);
            cache.put(cacheKey, response);
            return response;
            
        } catch (Exception e) {
            metrics.recordRequest(false);
            return createFallbackNPCResponse(npcName, playerInput);
        }
    }
    
    /**
     * Generate world event descriptions for autonomous events.
     */
    public AIResponse generateWorldEvent(String eventType, String context) {
        if (!config.isConfigured()) {
            return createFallbackWorldEvent(eventType);
        }
        
        try {
            var prompt = buildWorldEventPrompt(eventType, context);
            var response = callClaudeAPI(prompt);
            
            metrics.recordRequest(true);
            return response;
            
        } catch (Exception e) {
            metrics.recordRequest(false);
            return createFallbackWorldEvent(eventType);
        }
    }
    
    private AIResponse callClaudeAPI(String prompt) throws IOException, InterruptedException {
        var requestBody = objectMapper.writeValueAsString(Map.of(
            "model", config.getClaudeModel(),
            "max_tokens", config.getMaxTokens(),
            "temperature", config.getTemperature(),
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
            .header("x-api-key", config.getClaudeApiKey())
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
        
        return new AIResponse(
            content,
            true,
            null,
            usage.path("input_tokens").asInt(),
            usage.path("output_tokens").asInt(),
            Instant.now()
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
        
        return new AIResponse(
            fallbackText,
            false,
            reason,
            0,
            0,
            Instant.now()
        );
    }
    
    private AIResponse createFallbackNPCResponse(String npcName, String playerInput) {
        var fallbackText = String.format("💬 %s considera le tue parole pensierosamente, anche se la sua risposta sembra distante oggi.", npcName);
        
        return new AIResponse(
            fallbackText,
            false,
            "AI service not available",
            0,
            0,
            Instant.now()
        );
    }
    
    private AIResponse createFallbackWorldEvent(String eventType) {
        var fallbackText = String.format("🌍 Un %s si verifica nel mondo, i suoi effetti si propagano attraverso il regno.", eventType);
        
        return new AIResponse(
            fallbackText,
            false,
            "AI service not available",
            0,
            0,
            Instant.now()
        );
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
}