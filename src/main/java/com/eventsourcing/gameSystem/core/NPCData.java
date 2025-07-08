package com.eventsourcing.gameSystem.core;

import java.util.List;
import java.util.Map;

/**
 * NPC data for any game system.
 */
public record NPCData(
    String id,
    String name,
    String race,
    String occupation,
    String personality,
    String motivation,
    String appearance,
    List<String> knowledge,
    Map<String, String> relationships
) {}