package com.eventsourcing.core.infrastructure;

import com.eventsourcing.core.domain.DomainEvent;
import java.util.List;

/**
 * Result of appending events to a stream.
 */
public sealed interface AppendResult<TEvent extends DomainEvent> {
    record Success<TEvent extends DomainEvent>(List<StoredEvent<TEvent>> events) implements AppendResult<TEvent> {}
    record ConcurrentModification<TEvent extends DomainEvent>(long actualVersion) implements AppendResult<TEvent> {}
}
