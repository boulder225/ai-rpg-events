package com.eventsourcing.core.infrastructure;

import com.eventsourcing.core.domain.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Utility functions for event sourcing operations.
 */
public class EventSourcing {
    
    /**
     * Rebuild state from a sequence of events.
     */
    public static <TState, TEvent extends DomainEvent> TState fromEvents(
        TState initialState,
        List<StoredEvent<TEvent>> events,
        BiFunction<TState, TEvent, TState> applier) {
        
        return events.stream()
            .map(StoredEvent::event)
            .reduce(initialState, applier, (s1, s2) -> s2);
    }
    
    /**
     * Rebuild state from events up to a specific point in time.
     */
    public static <TState, TEvent extends DomainEvent> TState fromEventsUntil(
        TState initialState,
        List<StoredEvent<TEvent>> events,
        Instant pointInTime,
        BiFunction<TState, TEvent, TState> applier) {
        
        return events.stream()
            .filter(e -> e.timestamp().isBefore(pointInTime) || e.timestamp().equals(pointInTime))
            .map(StoredEvent::event)
            .reduce(initialState, applier, (s1, s2) -> s2);
    }
    
    /**
     * Get the version from a list of events.
     */
    public static <TEvent extends DomainEvent> long getVersion(List<StoredEvent<TEvent>> events) {
        return events.isEmpty() ? ExpectedVersion.NO_STREAM : events.get(events.size() - 1).streamVersion();
    }
}
