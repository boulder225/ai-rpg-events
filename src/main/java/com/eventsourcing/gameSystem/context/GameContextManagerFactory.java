package com.eventsourcing.gameSystem.context;

import com.eventsourcing.gameSystem.plugins.GameSystemPlugin;
import com.eventsourcing.gameSystem.plugins.dnd.TSRBasicDnDPlugin;
import com.eventsourcing.ai.ClaudeAIService;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Factory for creating and configuring game context managers with appropriate plugins
 */
public class GameContextManagerFactory {
    
    private static final Logger LOGGER = Logger.getLogger(GameContextManagerFactory.class.getName());
    
    /**
     * Create a context manager for TSR Basic D&D with all appropriate plugins
     */
    public static GenericGameContextManager createTSRBasicDnDManager(ClaudeAIService aiService) {
        // File paths
        String gameDataFile = "src/main/resources/dnd/tsr_basic_adventure.json";
        
        // Create the context manager
        GenericGameContextManager contextManager = new GenericGameContextManager(
            gameDataFile, aiService
        );
        
        // Register D&D plugin
        GameSystemPlugin dndPlugin = new TSRBasicDnDPlugin();
        contextManager.registerPlugin(dndPlugin);
        
        LOGGER.info("Created TSR Basic D&D context manager with plugins: " + dndPlugin.getName());
        
        return contextManager;
    }
    
    /**
     * Create a generic context manager that can be configured with any plugins
     */
    public static GenericGameContextManager createGenericManager(
            String gameDataFile, 
            ClaudeAIService aiService) {
        
        return new GenericGameContextManager(gameDataFile, aiService);
    }
    
    /**
     * Add plugins to an existing context manager based on game system type
     */
    public static void configurePluginsForGameSystem(
            GenericGameContextManager contextManager, 
            String gameSystemType) {
        
        switch (gameSystemType.toLowerCase()) {
            case "tsr_basic_dnd":
            case "dnd":
            case "basic_dnd":
                contextManager.registerPlugin(new TSRBasicDnDPlugin());
                break;
            
            // Add other game systems here in the future
            // case "pathfinder":
            //     contextManager.registerPlugin(new PathfinderPlugin());
            //     break;
            
            default:
                LOGGER.warning("Unknown game system type: " + gameSystemType);
        }
    }
}
