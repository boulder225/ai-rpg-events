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
                return createContextualFallback(playerAction);
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
                return createContextualFallback(playerAction, "Rate limit exceeded");
            }
            
            try {
                var prompt = buildGameMasterPrompt(context, playerAction);
                log.info("Calling Claude API for GM response: requestId={}, promptLength={}", requestId, prompt.length());
                var response = callClaudeAPI(prompt);
                
                switch (response) {
                    case AIResponse.Success success -> {
                        // Simple response validation
                        if (success.content().length() < 10) {
                            log.warn("AI response too short, using fallback: requestId={}", requestId);
                            return createContextualFallback(playerAction, "Response too short");
                        }
                        
                        log.info("GM response successful: requestId={}, tokens={}, contentLength={}", 
                            requestId, success.getTotalTokens(), success.content().length());
                        metrics.recordRequest(true);
                        cache.put(cacheKey, response);
                        return response;
                    }
                    case AIResponse.Error error -> {
                        log.error("GM response failed: requestId={}, error={}", requestId, error.errorMessage());
                        metrics.recordRequest(false);
                        return createContextualFallback(playerAction, error.errorMessage());
                    }
                    default -> {
                        log.warn("Unexpected GM response type: requestId={}, type={}", requestId, response.getClass().getSimpleName());
                        return response;
                    }
                }
                
            } catch (Exception e) {
                log.error("GM API call failed: requestId={}", requestId, e);
                metrics.recordRequest(false);
                return createContextualFallback(playerAction, "AI service temporarily unavailable: " + e.getMessage());
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
        // Simple token validation - keep context reasonable
        if (context.length() > 3000) {
            context = truncateContextSimple(context);
        }
        
        // Get language from system or default to Italian
        String language = System.getProperty("rpg.language", "it");
        var template = getPromptTemplate(language);
        
        return String.format(template, context, playerAction, getDifficultyLevel());
    }
    
    private String getPromptTemplate(String language) {
        return switch (language.toLowerCase()) {
            case "it" -> """
                Sei un Game Master AI per TSR Basic D&D ambientato in un mondo fantasy.
                
                CONTESTO DI GIOCO:
                %s
                
                AZIONE DEL GIOCATORE: %s
                DIFFICOLTÀ: %s
                
                Rispondi in 2-3 frasi vivide e immersive che:
                • Riconoscano l'azione del giocatore con conseguenze appropriate
                • Mantengano coerenza con le regole di D&D Basic
                • Includano dettagli sensoriali specifici della location
                • Offrano 1-2 opzioni di azione chiare per continuare
                • Usino un tono narrativo coinvolgente ma non drammatico
                
                Risposta del Game Master:
                """;
            case "en" -> """
                You are a Game Master AI for TSR Basic D&D set in a fantasy world.
                
                GAME CONTEXT:
                %s
                
                PLAYER ACTION: %s
                DIFFICULTY: %s
                
                Respond in 2-3 vivid, immersive sentences that:
                • Acknowledge the player's action with appropriate consequences
                • Maintain consistency with D&D Basic rules
                • Include location-specific sensory details
                • Offer 1-2 clear action options to continue
                • Use engaging but not overly dramatic narrative tone
                
                Game Master Response:
                """;
            default -> getPromptTemplate("en"); // Fallback to English
        };
    }
    
    private String truncateContextSimple(String context) {
        // Keep the most important parts for AI processing
        var lines = context.split("\n");
        var important = java.util.Arrays.stream(lines)
            .filter(line -> line.contains("LOCATION") || 
                           line.contains("CHARACTER") ||
                           line.contains("RECENT") ||
                           line.contains("HEALTH") ||
                           line.contains("EQUIPMENT"))
            .limit(12) // Keep reasonable context size
            .collect(java.util.stream.Collectors.joining("\n"));
        
        return important + "\n[Context optimized for AI processing]";
    }
    
    private String getDifficultyLevel() {
        // Simple difficulty assessment - could be made configurable
        return "Normale"; // Basic, Normal, Hard
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
    
    private AIResponse createContextualFallback(String playerAction) {
        return createContextualFallback(playerAction, "AI service not configured");
    }
    
    private AIResponse createContextualFallback(String playerAction, String reason) {
        String actionType = extractActionType(playerAction);
        String language = System.getProperty("rpg.language", "it");
        
        var fallbackText = switch (language.toLowerCase()) {
            case "it" -> createItalianFallback(actionType, playerAction);
            case "en" -> createEnglishFallback(actionType, playerAction);
            default -> createItalianFallback(actionType, playerAction);
        };
        
        return AIResponse.fallback(fallbackText, reason);
    }
    
    private String createItalianFallback(String actionType, String playerAction) {
        return switch (actionType) {
            case "move", "go" -> "🚶 Ti muovi attraverso l'ambiente con cautela, osservando i cambiamenti nel paesaggio circostante. Puoi continuare ad esplorare o fermarti per esaminare meglio.";
            case "attack" -> "⚔️ Ti prepari per il combattimento, stringendo la tua arma. L'adrenalina scorre mentre valuti la situazione. Puoi attaccare direttamente o cercare una strategia migliore.";
            case "search", "look", "examine" -> "👁️ Esamini attentamente l'area, cercando dettagli che potrebbero essere utili. Le ombre nascondono segreti che attendono di essere scoperti. Puoi guardare più da vicino o spostarti altrove.";
            case "take", "pickup" -> "🤏 Raccogli l'oggetto con attenzione, sentendone il peso e la texture. Potrebbe essere utile nel tuo viaggio. Puoi esaminarlo meglio o continuare la tua esplorazione.";
            case "talk", "speak" -> "💬 Inizi una conversazione, scegliendo le parole con cura. L'interazione sociale può rivelare informazioni preziose. Puoi approfondire il dialogo o salutare educatamente.";
            case "rest", "sleep" -> "😴 Ti fermi per riposare, recuperando energie per le sfide future. Il tempo di riposo ti permette di riflettere sui tuoi progressi. Puoi continuare a riposare o riprendere il viaggio.";
            default -> "✨ Il mondo fantasy risponde alla tua azione '" + playerAction + "' con misteriosa energia. Senti che qualcosa è cambiato, anche se i dettagli rimangono velati. Puoi provare un'altra azione o osservare meglio.";
        };
    }
    
    private String createEnglishFallback(String actionType, String playerAction) {
        return switch (actionType) {
            case "move", "go" -> "🚶 You move carefully through the environment, observing changes in the surrounding landscape. You can continue exploring or stop to examine more closely.";
            case "attack" -> "⚔️ You prepare for combat, gripping your weapon firmly. Adrenaline flows as you assess the situation. You can attack directly or seek a better strategy.";
            case "search", "look", "examine" -> "👁️ You carefully examine the area, looking for details that might be useful. Shadows hide secrets waiting to be discovered. You can look more closely or move elsewhere.";
            case "take", "pickup" -> "🤏 You carefully pick up the object, feeling its weight and texture. It might be useful on your journey. You can examine it more closely or continue exploring.";
            case "talk", "speak" -> "💬 You begin a conversation, choosing your words carefully. Social interaction can reveal valuable information. You can deepen the dialogue or politely say goodbye.";
            case "rest", "sleep" -> "😴 You stop to rest, recovering energy for future challenges. Rest time allows you to reflect on your progress. You can continue resting or resume your journey.";
            default -> "✨ The fantasy world responds to your action '" + playerAction + "' with mysterious energy. You sense something has changed, though details remain veiled. You can try another action or observe more carefully.";
        };
    }
    
    private String extractActionType(String playerAction) {
        String action = playerAction.toLowerCase().trim();
        
        if (action.startsWith("/")) {
            action = action.substring(1);
        }
        
        if (action.startsWith("go ") || action.startsWith("move ") || action.contains("vai ") || action.contains("muovi")) {
            return "move";
        } else if (action.contains("attack") || action.contains("fight") || action.contains("attacca") || action.contains("combatti")) {
            return "attack";
        } else if (action.contains("look") || action.contains("search") || action.contains("examine") || action.contains("guarda") || action.contains("cerca") || action.contains("esamina")) {
            return "search";
        } else if (action.contains("take") || action.contains("pickup") || action.contains("get") || action.contains("prendi") || action.contains("raccogli")) {
            return "take";
        } else if (action.contains("talk") || action.contains("speak") || action.contains("say") || action.contains("parla") || action.contains("di'")) {
            return "talk";
        } else if (action.contains("rest") || action.contains("sleep") || action.contains("riposa") || action.contains("dormi")) {
            return "rest";
        } else {
            return "generic";
        }
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