package com.eventsourcing.core.infrastructure;

import com.eventsourcing.core.domain.DomainEvent;
import java.time.Instant;

/**
 * Event with metadata as stored in the event store.
 */
public record StoredEvent<TEvent extends DomainEvent>(
    TEvent event,
    long streamVersion,
    Instant timestamp,
    String streamId
) {}
