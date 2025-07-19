package com.eventsourcing.api;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launcher for the AI-RPG Event Sourcing API Server.
 * Brings together event sourcing with RESTful endpoints for epic adventures!
 */
public class RPGServerLauncher {
    private static final Logger log = LoggerFactory.getLogger(RPGServerLauncher.class);
    
    public static void main(String[] args) {
        int port = 8080;
        
        // Parse command line arguments
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                log.error("Invalid port number: {}", args[0]);
                System.exit(1);
            }
        }
        
        try {
            log.info("🎮 Starting AI-RPG Event Sourcing Platform...");
            log.info("🌐 Server will be available at: http://localhost:{}", port);
            log.info("");
            
            // Create and start the server
            var server = new RPGApiServer(port);
            
            // Add shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("\n🛑 Shutting down AI-RPG server...");
                server.stop();
                log.info("✅ Server shutdown complete. Adventure continues in the persistent event streams!");
            }));
            
            // Start the server
            server.start();
            
            log.info("✨ Event sourcing magic is active!");
            log.info("🎭 Autonomous AI agents are standing by...");
            log.info("📡 Press Ctrl+C to stop the server");
            
            // Keep the main thread alive
            Thread.currentThread().join();
            
        } catch (IOException e) {
            log.error("❌ Failed to start server: {}", e.getMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            log.info("🛑 Server interrupted");
            Thread.currentThread().interrupt();
        }
    }
}
