package com.eventsourcing.core.domain;

import java.time.Instant;

/**
 * Base interface for all commands in the system.
 * Commands represent intentions to change the domain state.
 */
public interface Command {
    String commandId();
    Instant issuedAt();
}
