package com.eventsourcing.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to load environment variables from .env file.
 * Automatically loads .env file if present and sets environment variables.
 */
public class EnvLoader {
    
    private static final Logger log = LoggerFactory.getLogger(EnvLoader.class);
    
    private static boolean loaded = false;
    
    /**
     * Load environment variables from .env file if it exists.
     * This should be called early in application startup.
     */
    public static synchronized void loadDotEnv() {
        if (loaded) {
            return;
        }
        
        try {
            Path envFile = Paths.get(".env");
            if (Files.exists(envFile)) {
                log.info("🔧 Loading environment variables from .env file");
                
                var envVars = parseDotEnvFile(envFile);
                envVars.forEach((key, value) -> {
                    // Only set if not already set in system environment
                    if (System.getenv(key) == null) {
                        System.setProperty(key, value);
                        // Update environment for current process
                        try {
                            setEnv(key, value);
                        } catch (Exception e) {
                            // Fallback to system property
                            System.setProperty(key, value);
                        }
                    }
                });
                
                log.info("✅ Loaded {} environment variables", envVars.size());
            } else {
                log.info("ℹ️ No .env file found, using system environment variables");
            }
        } catch (IOException e) {
            log.error("⚠️ Failed to load .env file: {}", e.getMessage());
        }
        
        loaded = true;
    }
    
    private static Map<String, String> parseDotEnvFile(Path envFile) throws IOException {
        var envVars = new HashMap<String, String>();
        
        Files.readAllLines(envFile).forEach(line -> {
            line = line.trim();
            
            // Skip comments and empty lines
            if (line.isEmpty() || line.startsWith("#")) {
                return;
            }
            
            // Parse KEY=VALUE format
            int equalsIndex = line.indexOf('=');
            if (equalsIndex > 0) {
                String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();
                
                // Remove quotes if present
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                } else if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }
                
                envVars.put(key, value);
            }
        });
        
        return envVars;
    }
    
    /**
     * Set environment variable for current process.
     * Uses reflection to modify the environment map.
     */
    @SuppressWarnings("unchecked")
    private static void setEnv(String key, String value) throws Exception {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            var theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.put(key, value);
            
            var theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.put(key, value);
        } catch (NoSuchFieldException e) {
            // Fallback for different JVM implementations
            Class<?>[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for (Class<?> cl : classes) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    var field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.put(key, value);
                }
            }
        }
    }
}