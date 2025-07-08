package com.eventsourcing.gameSystem.core;

/**
 * Quick command for frontend UI.
 */
public record QuickCommand(
    String label,
    String command,
    String description
) {
    
    /**
     * Create a quick command.
     */
    public static QuickCommand of(String label, String command, String description) {
        return new QuickCommand(label, command, description);
    }
}