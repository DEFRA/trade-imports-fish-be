# Trade Imports Fish Backend Documentation

This documentation covers the technical architecture, components, and operational procedures for the Trade Imports Fish Backend service.

## Overview

The Trade Imports Fish Backend is a Spring Boot 3.5 REST API service built with Java 25, demonstrating CDP (Centralized Delivery Platform) compliance. It provides CRUD operations for trade import data with MongoDB persistence, AWS integration, and structured logging.

## Documentation Structure

- [Architecture & Setup](architecture.md) — Project structure, technology stack, local development environment with Docker Compose
- [API & Routes](api.md) — REST endpoints, request/response specifications, OpenAPI documentation
- [Core Services](services.md) — Business logic, example service, data persistence layer
- [Configuration](configuration.md) — Environment variables, MongoDB setup, AWS configuration, logging, actuator endpoints
- [Error Handling](error-handling.md) — Exception handling strategy, HTTP status codes, ProblemDetail responses, trace ID correlation
- [Filters & Middleware](filters.md) — Request tracing, health check bypass, MDC population for structured logging
- [Monitoring & Observability](monitoring.md) — CloudWatch metrics, structured logging, health checks, Micrometer integration
