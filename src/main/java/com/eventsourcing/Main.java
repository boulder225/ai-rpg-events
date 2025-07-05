package com.eventsourcing;

import com.eventsourcing.api.RPGApiServer;

/**
 * Main entry point for the AI-RPG Event Sourcing Platform.
 */
public class Main {
    
    public static void main(String[] args) {
        try {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
            
            System.out.println("🎮 Starting AI-RPG Event Sourcing Platform...");
            System.out.println("🌐 Server will be available at: http://localhost:" + port);
            System.out.println();
            
            var server = new RPGApiServer(port);
            server.start();
            
            System.out.println();
            System.out.println("✅ Server started successfully!");
            System.out.println("📚 API Documentation:");
            System.out.println("  POST /api/session/create - Create new adventure session");
            System.out.println("  POST /api/game/action - Execute game actions with AI responses");
            System.out.println("  GET  /api/game/status - Get complete world state");
            System.out.println("  GET  /api/ai/prompt - View AI context prompt");
            System.out.println("  GET  /api/metrics - System performance metrics");
            System.out.println("  GET  / - Interactive web interface");
            System.out.println();
            System.out.println("🛑 Press Ctrl+C to stop the server");
            
            // Keep the server running
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("❌ Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
} 