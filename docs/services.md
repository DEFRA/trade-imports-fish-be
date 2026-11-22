# Core Services

## Service Architecture

The service layer contains business logic, data persistence, and domain-specific operations. All services are Spring-managed beans and follow single responsibility principle.

## ExampleService

The `ExampleService` implements CRUD operations for `Example` entities with conflict detection, metrics publishing, and comprehensive error handling.

### Dependencies

- `ExampleRepository` — Spring Data MongoDB repository for data access
- `MeterRegistry` — Micrometer for publishing metrics

### Methods

#### create(Example entity) → Example

Create a new example with duplicate name detection.

**Behavior**:

1. Check if example with same name already exists
2. If exists, throw `ConflictException` (409)
3. Set `created` timestamp to current time
4. Save to MongoDB
5. Increment `example_created` metric
6. Log success with example ID
7. Return saved entity with generated MongoDB ObjectId

**Exception Handling**:

- `ConflictException` — name already exists (custom application exception)
- `DuplicateKeyException` — handled from MongoDB to `ConflictException` (race condition mitigation)

**Example**:

```java
Example entity = new Example("unique-name", "value");
Example saved = exampleService.create(entity);
// saved.getId() -> "507f1f77bcf86cd799439011"
// saved.getCreated() -> LocalDateTime.now()
```

#### findAll() → List<Example>

Retrieve all examples from MongoDB.

**Behavior**:

1. Query MongoDB collection for all documents
2. Log count of results
3. Return list (empty if no documents)

**Example**:

```java
List<Example> examples = exampleService.findAll();
// returns all examples, or empty list if none exist
```

#### findById(String id) → Example

Retrieve a single example by MongoDB ObjectId.

**Behavior**:

1. Query MongoDB by ID
2. If not found, throw `NotFoundException` (404)
3. Log and return entity

**Exception Handling**:

- `NotFoundException` — ID does not exist

**Example**:

```java
Example found = exampleService.findById("507f1f77bcf86cd799439011");
// throws NotFoundException if ID doesn't exist
```

#### update(String id, Example entity) → Example

Update an existing example with conflict detection for name changes.

**Behavior**:

1. Fetch existing entity (throws `NotFoundException` if not found)
2. If name is changing to a different value:
   - Check if new name exists on another example
   - If conflict found, throw `ConflictException` (409)
3. Update fields: `name`, `value`, `counter`
4. Preserve `created` timestamp (not updated)
5. Save to MongoDB
6. Increment `example_updated` metric
7. Log success
8. Return updated entity

**Exception Handling**:

- `NotFoundException` — ID doesn't exist
- `ConflictException` — new name conflicts with another example

**Example**:

```java
Example updated = exampleService.update(
  "507f1f77bcf86cd799439011",
  new Example("new-name", "new-value")
);
// updated.getCreated() -> unchanged
```

#### delete(String id) → void

Delete an example by ID.

**Behavior**:

1. Check if example exists (throws `NotFoundException` if not)
2. Delete from MongoDB
3. Increment `example_deleted` metric
4. Log success

**Exception Handling**:

- `NotFoundException` — ID doesn't exist

**Example**:

```java
exampleService.delete("507f1f77bcf86cd799439011");
// throws NotFoundException if doesn't exist
```

---

## Domain Model

### Example Entity

MongoDB document representing a trade import example.

**Collection**: `examples`

**Fields**:

- `id` (String) — MongoDB ObjectId, auto-generated, unique
- `name` (String) — Unique identifier, required, non-blank
- `value` (String) — Required, non-blank
- `counter` (Integer) — Optional
- `created` (LocalDateTime) — Timestamp set at creation

**Indexes**:

- Unique index on `name` (enforced via `@Indexed(unique = true)`)
- MongoDB automatically creates index on `_id`

**Validation** (Jakarta Beans Validation):

- `@NotBlank` on `name` and `value`

**Example JSON**:

```json
{
  "id": "507f1f77bcf86cd799439011",
  "name": "trade-example-001",
  "value": "import data",
  "counter": 42,
  "created": "2024-11-21T13:16:51"
}
```

---

## Repository Layer

### ExampleRepository

Spring Data MongoDB repository providing data access methods.

**Interface**:

```java
public interface ExampleRepository extends MongoRepository<Example, String> {
    Optional<Example> findByName(String name);
}
```

**Methods**:

- `findById(String id)` — Inherited from `CrudRepository`
- `findAll()` — Inherited from `CrudRepository`
- `save(Example)` — Inherited from `CrudRepository`
- `deleteById(String id)` — Inherited from `CrudRepository`
- `findByName(String name)` — Custom query on unique index

**MongoDB Mapping**:

- Automatically maps `Example` class to MongoDB collection `examples`
- ObjectId generated automatically on save (if not set)
- Supports all CRUD operations via Spring Data

---

## Metrics

The service instruments custom counters via Micrometer:

| Metric                                   | Type    | Description                                                  |
| ---------------------------------------- | ------- | ------------------------------------------------------------ |
| `example_created`                        | Counter | Incremented on successful creation; not published by default |
| `example_updated`                        | Counter | Incremented on successful update; not published by default   |
| `example_deleted`                        | Counter | Incremented on successful deletion; not published by default |
| `controller.getAllExampleEntities.count` | Counter | Incremented on each list request                             |
| `controller.postExampleEntity.time`      | Timer   | Time spent in create endpoint                                |
| `controller.getExampleEntity.time`       | Timer   | Time spent in get-by-id endpoint                             |
| `controller.getAllExampleEntities.time`  | Timer   | Time spent in list endpoint                                  |
| `controller.putExampleEntity.time`       | Timer   | Time spent in update endpoint                                |
| `controller.deleteExampleEntity.time`    | Timer   | Time spent in delete endpoint                                |

Metrics are published to CloudWatch via EMF (see [Monitoring & Observability](monitoring.md)).

---

## Logging Strategy

Services use structured logging (SLF4J with ECS format):

```java
log.info("Creating example with name: {}", entity.getName());
// Structured output: timestamp, level, logger name, trace.id (if supplied), message
```

**Log Levels**:

- `INFO` — Business events (create, update, delete success)
- `WARN` — Conflicts, not found, expected errors
- `DEBUG` — Entry/exit of methods, detailed context
- `ERROR` — Unexpected exceptions with stack trace

**MDC Context** (see [Filters & Middleware](filters.md)):

- `trace.id` — Nonblank trace ID supplied in the configured request header
- `http.request.method` — HTTP method (GET, POST, etc.)
- `url.full` — Request URL without the query string

The filter records the response status only after the request chain completes and clears the MDC without logging it. Controller and service log events therefore do not include the final response status.

---

## Example Workflow

**Create → Update → Delete**:

```bash
# 1. Create
POST /example
{ "name": "trade-001", "value": "import" }
← 201 Created: { "id": "507f...", "created": "2024-11-21T13:16:51" }

# 2. List
GET /example
← 200 OK: [{ "id": "507f...", "name": "trade-001", ... }]

# 3. Get single
GET /example/507f1f77bcf86cd799439011
← 200 OK: { "id": "507f...", ... }

# 4. Update
PUT /example/507f1f77bcf86cd799439011
{ "name": "trade-001-v2", "value": "updated", "counter": 1 }
← 200 OK: { "id": "507f...", "created": "2024-11-21T13:16:51" }

# 5. Delete
DELETE /example/507f1f77bcf86cd799439011
← 204 No Content

# 6. Verify deleted
GET /example/507f1f77bcf86cd799439011
← 404 Not Found: { "type": "not-found", "title": "Resource Not Found" }
```

**Illustrative metrics output** (CloudWatch; requires explicitly enabling each `example_*` counter in the metrics allowlist):

```
example_created: 1
example_updated: 1
example_deleted: 1
controller.getAllExampleEntities.count: 1
```
