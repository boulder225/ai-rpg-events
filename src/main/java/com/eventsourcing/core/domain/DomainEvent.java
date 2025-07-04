package com.eventsourcing.core.domain;

import java.time.Instant;

/**
 * Base interface for all domain events in the system.
 * Events are immutable facts about what happened in the domain.
 */
public interface DomainEvent {
    String eventId();
    Instant occurredAt();
    
    default int version() { 
        return 1; 
    }
}
