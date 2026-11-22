# [DEVOXXGENIE.md](http://DEVOXXGENIE.md)

## Project Guidelines

This project is built on a microservices architecture. Each service is independently deployable and owns its PostgreSQL database. Service boundaries align with DDD bounded contexts. Inter-service communication relies exclusively on Kafka events (eventual consistency); synchronous calls between services are forbidden.

## Build Commands

- **Build all modules:** `./gradlew build`
- **Build specific service:** `./gradlew :service-order:build`
- **Run all tests:** `./gradlew test`
- **Run tests for a service:** `./gradlew :service-order:test`
- **Run a single test class:** `./gradlew :service-order:test --tests "*OrderServiceTest"`
- **Run Checkstyle only:** `./gradlew checkstyleMain`
- **Run service locally:** `./gradlew :service-order:bootRun`



## Code Style

- Follow Google Java Style Guide, enforced by Checkstyle (`config/checkstyle/checkstyle.xml`).
- Indentation: 4 spaces. Maximum line length: 120 characters.
- Always use braces for control statements (`if`, `for`, `while`), even for single statements.
- Import order: `java`/`javax` first, then third-party libraries, then project-internal packages.
- No unused imports. No wildcard imports.
- Constructor injection only via `@RequiredArgsConstructor`. Field injection is forbidden.
- DTOs are Java records with Jakarta Bean Validation annotations.
- Entities use only `@Getter` and `@NoArgsConstructor(access = PROTECTED)`. `@Data` is forbidden.
- Entity ↔ DTO mapping is done exclusively via MapStruct. Manual mapping is forbidden.



## Dependencies

Versions are managed centrally in `gradle/libs.versions.toml`.

- **Java:** 25
- **Spring Boot:** 3.5.x (BOM)
- **Spring Cloud:** 2025.x (BOM)
- **Spring Kafka / Spring Data Redis / Spring Data JPA:** Managed by Spring Boot BOM
- **PostgreSQL Driver:** Managed by Spring Boot BOM
- **Flyway:** `flyway-core` + `flyway-database-postgresql` (managed by Spring Boot BOM)
- **MapStruct:** 1.6.x (`mapstruct` + `mapstruct-processor`)
- **Lombok:** `lombok` + `lombok-mapstruct-binding` (managed by Spring Boot BOM)
- **Testcontainers:** `testcontainers-postgresql`, `testcontainers-kafka`, `testcontainers-redis`
- **JUnit 5 / AssertJ / Mockito:** `spring-boot-starter-test`

**Note:** With Gradle Groovy DSL, annotationProcessor order must be: Lombok before MapStruct.

## Project Structure

Each service has its own `db/migration` directory. Shared databases across services are forbidden.

## Architecture Insights



### Kafka & Eventual Consistency

- Kafka events must be published **after** the database transaction commits. Use `@TransactionalEventListener(phase = AFTER_COMMIT)` or the Outbox pattern.
- Consumers must be idempotent. Use `eventId` or an `idempotency key` for deduplication.
- Topic naming: `{domain}.{entity}.{action}`, e.g. `order.order.created`.
- Never call another service's API synchronously inside a transaction.



### Redis Caching

- Redis is for caching and ephemeral state only. PostgreSQL remains the source of truth.
- Every cache entry must have an explicit TTL. Permanent caches are forbidden.
- Key naming: `{service}:{entity}:{id}`, e.g. `order:order:12345`.



### Flyway Migrations

- All schema changes go through Flyway. Manual database modifications are forbidden.
- Migrations must be backward compatible: no dropping columns; new `NOT NULL` columns require a `DEFAULT` within the same release.
- Naming: `V{version}__{description}.sql`, e.g. `V1__create_order_table.sql`.
- `spring.flyway.clean-disabled=true` in all environments.



### Virtual Threads (Java 25)

- `spring.threads.virtual.enabled=true` is enabled globally.
- For outbound HTTP calls, use `RestClient` with `Executors.newVirtualThreadPerTaskExecutor()`.
- Be aware of HTTP/1.1 connection limits; configure the HTTP client pool accordingly.



## Testing Conventions

- Unit tests: JUnit 5 + Mockito for service-layer isolation.
- Integration tests: Testcontainers (PostgreSQL, Kafka, Redis).
- Kafka consumer tests must cover: happy path, duplicate messages (idempotency), malformed messages.
- Redis cache tests must cover: cache hit, cache miss, TTL expiry.
- Test method naming: `methodName_condition_expectedResult`.

