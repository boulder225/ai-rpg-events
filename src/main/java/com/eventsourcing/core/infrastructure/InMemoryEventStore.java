package com.eventsourcing.core.infrastructure;

import com.eventsourcing.core.domain.DomainEvent;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * In-memory implementation of EventStore for development and testing.
 * Thread-safe with proper concurrency control.
 */
public class InMemoryEventStore<TEvent extends DomainEvent> implements EventStore<TEvent> {
    
    private final Map<String, List<StoredEvent<TEvent>>> streams = new ConcurrentHashMap<>();
    private final Map<String, ReentrantReadWriteLock> streamLocks = new ConcurrentHashMap<>();
    
    @Override
    public AppendResult<TEvent> appendToStream(StreamId streamId, ExpectedVersion expectedVersion, List<TEvent> events) {
        if (events.isEmpty()) {
            return new AppendResult.Success<>(List.of());
        }
        
        var lock = streamLocks.computeIfAbsent(streamId.value(), k -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        
        try {
            var currentEvents = streams.computeIfAbsent(streamId.value(), k -> new ArrayList<>());
            long currentVersion = currentEvents.isEmpty() ? ExpectedVersion.NO_STREAM : currentEvents.size() - 1;
            
            // Check expected version
            if (!isVersionMatch(expectedVersion, currentVersion, currentEvents.isEmpty())) {
                return new AppendResult.ConcurrentModification<>(currentVersion);
            }
            
            // Append new events
            List<StoredEvent<TEvent>> storedEvents = new ArrayList<>();
            for (int i = 0; i < events.size(); i++) {
                var event = events.get(i);
                var storedEvent = new StoredEvent<>(
                    event,
                    currentVersion + i + 1,
                    event.occurredAt(),
                    streamId.value()
                );
                currentEvents.add(storedEvent);
                storedEvents.add(storedEvent);
            }
            
            return new AppendResult.Success<>(storedEvents);
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public List<StoredEvent<TEvent>> readStream(StreamId streamId) {
        var lock = streamLocks.computeIfAbsent(streamId.value(), k -> new ReentrantReadWriteLock());
        lock.readLock().lock();
        
        try {
            return new ArrayList<>(streams.getOrDefault(streamId.value(), List.of()));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<StoredEvent<TEvent>> readStreamUntil(StreamId streamId, Instant pointInTime) {
        return readStream(streamId).stream()
            .filter(e -> e.timestamp().isBefore(pointInTime) || e.timestamp().equals(pointInTime))
            .toList();
    }
    
    @Override
    public List<StoredEvent<TEvent>> readStreamFromVersion(StreamId streamId, long fromVersion) {
        return readStream(streamId).stream()
            .filter(e -> e.streamVersion() >= fromVersion)
            .toList();
    }
    
    @Override
    public long getStreamVersion(StreamId streamId) {
        var events = readStream(streamId);
        return events.isEmpty() ? ExpectedVersion.NO_STREAM : events.get(events.size() - 1).streamVersion();
    }
    
    private boolean isVersionMatch(ExpectedVersion expected, long actual, boolean streamEmpty) {
        return switch (expected) {
            case ExpectedVersion.Any() -> true;
            case ExpectedVersion.NoStream() -> actual == ExpectedVersion.NO_STREAM;
            case ExpectedVersion.EmptyStream() -> streamEmpty;
            case ExpectedVersion.Exact(long version) -> actual == version;
        };
    }
}
