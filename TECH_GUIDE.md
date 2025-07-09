# AI-RPG Event Sourcing Platform Technical Guide

This document provides a high level overview of the codebase structure, major components and how the application layers interact. It is meant as a starting point for developers getting familiar with the project.

## 1. Project Layout

```
/ (repo root)
├── src/main/java           # Java source files
│   └── com/eventsourcing   # Main package
│       ├── core            # Generic event sourcing infrastructure
│       ├── ai              # Claude AI integration layer
│       ├── api             # HTTP API server
│       ├── gameSystem      # Pluggable game system framework
│       ├── dnd             # D&D focused helpers
│       └── rpg             # Domain logic and state models
├── src/test/java           # Unit/integration tests
├── frontend                # React based UI
└── README.md               # Detailed usage instructions
```

## 2. Core Event Sourcing

The `com.eventsourcing.core` packages implement a minimal event sourcing toolkit used by the RPG domain. Important classes include:

- `DomainEvent`, `Command` and `CommandResult` – base interfaces for events and commands.
- `EventStore` interface with an `InMemoryEventStore` implementation supporting optimistic concurrency.
- `EventSourcing` utility with helpers to rebuild state from a list of `StoredEvent` instances.

These utilities are general and not specific to the RPG domain.

## 3. RPG Domain Layer

The `com.eventsourcing.rpg` package defines the domain events, commands and state models for the game world. Highlights:

- `RPGEvent` – sealed interface enumerating all event types (player actions, NPC changes, world events, etc.).
- `RPGCommand` – commands that trigger domain logic.
- `RPGBusinessLogic` – static methods processing commands and producing events.
- `RPGCommandHandler` – orchestrates command execution: loads the event stream, rebuilds state, calls the business logic and appends resulting events.
- `RPGState` – read models such as `PlayerState`, `NPCState`, `LocationState` built from events.
- `AIAgentExample` and `RPGDemo` – demonstration classes showing autonomous behaviour and end‑to‑end flow.

## 4. AI Integration

Package `com.eventsourcing.ai` encapsulates interaction with Claude AI and fallback behaviour. Key classes:

- `AIConfig` – loads configuration and rate limit parameters from environment variables.
- `ClaudeAIService` – main class used by the API server to obtain AI generated responses. If the API key is missing, it provides deterministic fallback text.
- `AICache`, `RateLimiter` and `AIMetrics` – support caching, throttling and metrics collection for AI calls.
- `AIResponse` – value object representing results from Claude or fallback generation.
- `EnvLoader` – optional loader for a `.env` file.

## 5. Game System Plugins

`com.eventsourcing.gameSystem` defines a pluggable system for supporting multiple RPG rule sets. `GameSystem` is the main interface. The project includes a D&D Basic implementation in `plugins.dnd` with supporting data classes (`AdventureData`, `LocationData`, `NPCData`, etc.).

`GameSystemFactory` instantiates the desired game system based on configuration. The D&D plugin provides adventure data, quick commands and context generation used by the AI layer.

## 6. API Layer

`com.eventsourcing.api` exposes a lightweight HTTP server using `HttpServer` from the JDK.

- `RPGApiServer` defines REST endpoints under `/api/*` for creating sessions, processing actions and retrieving metrics.
- `RPGServerLauncher` bootstraps the server.
- `ApiModels` contains request/response DTOs.
- `RPGMetrics` collects stats for request counts and response times.

The server composes the core layers:

1. `EnvLoader` loads environment variables.
2. `RPGCommandHandler` with `InMemoryEventStore` handles persistence.
3. `ClaudeAIService` generates text for player commands.
4. `GameSystemFactory` provides domain rules and adventure data.

Responses include both the AI text and contextual data from `RPGState`.

## 7. Frontend

A small React application (`frontend/`) provides a minimal interface. `App.js` calls the backend API to create sessions, send player commands and render location maps. This UI is optional but useful for local demos.

## 8. Tests

Unit and integration tests under `src/test/java` verify the main components:

- `ClaudeAIServiceTest` ensures fallback behaviour without an API key.
- `RPGApiServerTest` starts an embedded server and checks the REST endpoints.
- `RPGSystemTest` validates command handling and state reconstruction logic.

Running `./gradlew test` executes all tests.

## 9. Interaction Flow

1. **Session creation** – client calls `/api/session/create` which creates a player stream and generates a welcome message (possibly using Claude AI).
2. **Player actions** – `/api/game/action` triggers `processGameActionWithAI` inside `RPGApiServer`. It builds context from the event store, asks `ClaudeAIService` for narration, then records resulting domain events through `RPGCommandHandler`.
3. **State queries** – `/api/game/status` reads state from events to present the world view; `/api/ai/prompt` exposes the AI context used for generation.
4. **Metrics** – `/api/metrics` aggregates stats from both the server and AI service.

The combination of these layers results in a persistent event-sourced RPG environment enhanced by AI generated storytelling.

