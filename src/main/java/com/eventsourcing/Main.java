package com.eventsourcing;

import com.eventsourcing.api.RPGApiServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the AI-RPG Event Sourcing Platform.
 */
public class Main {
    
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        try {
            // Heroku sets PORT environment variable
            String portEnv = System.getenv("PORT");
            int port = portEnv != null ? Integer.parseInt(portEnv) : 
                      (args.length > 0 ? Integer.parseInt(args[0]) : 8080);
            
            log.info("🎮 Starting AI-RPG Event Sourcing Platform...");
            log.info("🌐 Server will be available at: http://localhost:{}", port);
            
            var server = new RPGApiServer(port);
            server.start();
            
            log.info("✅ Server started successfully!");
            log.info("🛑 Press Ctrl+C to stop the server");
            
            // Keep the server running
            Thread.currentThread().join();
            
        } catch (Exception e) {
            log.error("❌ Failed to start server: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
} 