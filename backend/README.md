# asc-moda Backend

`asc-moda` is a Spring Boot microservices backend for a corporate e-commerce platform. This directory is the Maven multi-module backend root inside the monorepo.

## Module Structure

- `platform/config-server`: Centralized configuration service.
- `platform/discovery-server`: Eureka service registry.
- `platform/api-gateway`: Edge gateway for backend APIs.
- `shared/shared-kernel`: Cross-service primitives and contracts that are not domain-specific.
- `shared/shared-observability`: Shared logging, tracing, metrics, and actuator conventions.
- `shared/shared-test`: Shared test utilities for backend modules.
- `services/customer-service`: Customer profile and account boundary.
- `services/catalog-service`: Product catalog boundary.
- `services/inventory-service`: Stock and availability boundary.
- `services/cart-service`: Shopping cart boundary.
- `services/order-service`: Ordering boundary.
- `services/notification-service`: Email, messaging, and notification boundary.
- `services/search-service`: Search indexing and query boundary.
- `infra/docker-compose`: Local infrastructure for backend development.
- `scripts`: Backend automation scripts.

## Technology Stack

- Java 17
- Maven multi-module build
- Spring Boot 3.5.x
- Spring Cloud 2025.0.x
- Spring Cloud Config, Eureka, Gateway, OpenFeign, Resilience4j
- Spring Data JPA, Hibernate, Flyway, PostgreSQL
- Redis, RabbitMQ, Elasticsearch, Kibana, Keycloak
- SLF4J and Logback
- Docker Compose for local infrastructure

## Evolution Plan

1. Platform bootstrap: configuration, discovery, gateway, shared libraries, and local infrastructure.
2. Service foundations: database-per-service setup, Flyway baselines, security integration, and service contracts.
3. Domain implementation: customer, catalog, inventory, cart, order, notification, and search capabilities.
4. Operational hardening: observability, resilience policies, contract tests, security rules, and production configuration.
5. Frontend integration: API gateway routes and frontend-facing contracts once backend boundaries stabilize.

## Local Validation

From this directory:

```bash
mvn -DskipTests validate
```

Local infrastructure can be started from `infra/docker-compose`:

```bash
docker compose up -d
```
