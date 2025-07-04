package com.eventsourcing.core.infrastructure;

import com.eventsourcing.core.domain.DomainEvent;
import java.time.Instant;
import java.util.List;

/**
 * Generic event store interface for persisting and retrieving domain events.
 */
public interface EventStore<TEvent extends DomainEvent> {
    
    /**
     * Append events to a stream with optimistic concurrency control.
     */
    AppendResult<TEvent> appendToStream(StreamId streamId, ExpectedVersion expectedVersion, List<TEvent> events);
    
    /**
     * Read all events from a stream.
     */
    List<StoredEvent<TEvent>> readStream(StreamId streamId);
    
    /**
     * Read events from a stream up to a specific point in time.
     */
    List<StoredEvent<TEvent>> readStreamUntil(StreamId streamId, Instant pointInTime);
    
    /**
     * Read events from a specific version onwards.
     */
    List<StoredEvent<TEvent>> readStreamFromVersion(StreamId streamId, long fromVersion);
    
    /**
     * Get the current version of a stream.
     */
    long getStreamVersion(StreamId streamId);
}
