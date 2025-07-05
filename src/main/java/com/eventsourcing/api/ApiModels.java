package com.eventsourcing.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data transfer objects for the AI-RPG API.
 */
public class ApiModels {
    
    public static class SessionCreateRequest {
        @JsonProperty("player_id")
        public String playerId;
        
        @JsonProperty("player_name")
        public String playerName;
        
        public SessionCreateRequest() {}
        
        public SessionCreateRequest(String playerId, String playerName) {
            this.playerId = playerId;
            this.playerName = playerName;
        }
    }
    
    public static class GameActionRequest {
        @JsonProperty("session_id")
        public String sessionId;
        
        @JsonProperty("command")
        public String command;
        
        public GameActionRequest() {}
        
        public GameActionRequest(String sessionId, String command) {
            this.sessionId = sessionId;
            this.command = command;
        }
    }
    
    public static class GameResponse {
        @JsonProperty("success")
        public boolean success;
        
        @JsonProperty("message")
        public String message;
        
        @JsonProperty("session_id")
        public String sessionId;
        
        @JsonProperty("context")
        public Object context;
        
        @JsonProperty("error")
        public String error;
        
        public GameResponse() {}
        
        public GameResponse(boolean success, String message, String sessionId, Object context, String error) {
            this.success = success;
            this.message = message;
            this.sessionId = sessionId;
            this.context = context;
            this.error = error;
        }
    }
}
