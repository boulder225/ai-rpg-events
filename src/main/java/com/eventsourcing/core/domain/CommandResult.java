package com.eventsourcing.core.domain;

import java.util.List;

/**
 * Result of processing a command.
 * Either successful with generated events or failed with reason.
 */
public sealed interface CommandResult<TEvent extends DomainEvent> {
    record Success<TEvent extends DomainEvent>(List<TEvent> events) implements CommandResult<TEvent> {}
    record Failure<TEvent extends DomainEvent>(String reason) implements CommandResult<TEvent> {}
}
