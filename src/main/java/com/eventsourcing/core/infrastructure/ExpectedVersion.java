package com.eventsourcing.core.infrastructure;

/**
 * Version control for optimistic concurrency in event streams.
 */
public sealed interface ExpectedVersion {
    long NO_STREAM = -1;
    long EMPTY_STREAM = 0;
    
    record Any() implements ExpectedVersion {}
    record NoStream() implements ExpectedVersion {}
    record EmptyStream() implements ExpectedVersion {}
    record Exact(long version) implements ExpectedVersion {}
    
    static ExpectedVersion any() { 
        return new Any(); 
    }
    
    static ExpectedVersion exact(long version) { 
        return new Exact(version); 
    }
    
    static ExpectedVersion noStream() {
        return new NoStream();
    }
    
    static ExpectedVersion emptyStream() {
        return new EmptyStream();
    }
}
