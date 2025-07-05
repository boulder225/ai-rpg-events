package com.eventsourcing.api;

import java.io.IOException;

/**
 * Launcher for the AI-RPG Event Sourcing API Server.
 * Brings together event sourcing with RESTful endpoints for epic adventures!
 */
public class RPGServerLauncher {
    
    public static void main(String[] args) {
        int port = 8080;
        
        // Parse command line arguments
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.exit(1);
            }
        }
        
        try {
            System.out.println("🎮 Starting AI-RPG Event Sourcing Platform...");
            System.out.println("🌐 Server will be available at: http://localhost:" + port);
            System.out.println();
            
            // Create and start the server
            var server = new RPGApiServer(port);
            
            // Add shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Shutting down AI-RPG server...");
                server.stop();
                System.out.println("✅ Server shutdown complete. Adventure continues in the persistent event streams!");
            }));
            
            // Start the server
            server.start();
            
            System.out.println("✨ Event sourcing magic is active!");
            System.out.println("🎭 Autonomous AI agents are standing by...");
            System.out.println("📡 Press Ctrl+C to stop the server");
            
            // Keep the main thread alive
            Thread.currentThread().join();
            
        } catch (IOException e) {
            System.err.println("❌ Failed to start server: " + e.getMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            System.out.println("🛑 Server interrupted");
            Thread.currentThread().interrupt();
        }
    }
}
