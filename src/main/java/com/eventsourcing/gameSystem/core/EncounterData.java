package com.eventsourcing.gameSystem.core;

import java.util.List;
import java.util.Map;

/**
 * Encounter data for any game system.
 */
public record EncounterData(
    String id,
    String name,
    String type,
    String description,
    String trigger,
    int challengeRating,
    List<String> creatures,
    Map<String, String> tactics
) {}