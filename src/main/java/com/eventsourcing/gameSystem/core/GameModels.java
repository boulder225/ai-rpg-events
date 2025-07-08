package com.eventsourcing.gameSystem.core;

import java.util.List;
import java.util.Map;

/**
 * Supporting data models for game systems.
 * Contains package-private records used internally.
 */

/**
 * NPC data for any game system.
 */
record NPCData(
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

/**
 * Encounter data for any game system.
 */
record EncounterData(
    String id,
    String name,
    String type,
    String description,
    String trigger,
    int challengeRating,
    List<String> creatures,
    Map<String, String> tactics
) {}