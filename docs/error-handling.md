# Error Handling

## Exception Hierarchy

The service uses a custom exception hierarchy for domain-specific errors:

```
RuntimeException
├── ConflictException (409)
├── NotFoundException (404)
└── [Other runtime exceptions handled by GlobalExceptionHandler]
```

## Custom Exceptions

### ConflictException

HTTP Status: **409 Conflict**

Thrown when an operation conflicts with existing data.

**Causes**:

- Attempting to create an example with a name that already exists
- Attempting to update an example with a name that belongs to another example

**Example**:

```java
throw new ConflictException("Example with name 'duplicate' already exists");
```

**Response**:

```json
{
  "type": "https://api.cdp.defra.cloud/problems/conflict",
  "title": "Resource Conflict",
  "status": 409,
  "detail": "Example with name 'duplicate' already exists",
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### NotFoundException

HTTP Status: **404 Not Found**

Thrown when a resource does not exist.

**Causes**:

- Requesting an example by ID that doesn't exist
- Attempting to update/delete an example that doesn't exist

**Example**:

```java
throw new NotFoundException("Example not found with id: 507f1f77bcf86cd799439011");
```

**Response**:

```json
{
  "type": "https://api.cdp.defra.cloud/problems/not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Example not found with id: 507f1f77bcf86cd799439011",
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

## Global Exception Handler

The [`GlobalExceptionHandler`](https://github.com/DEFRA/trade-imports-fish-be/blob/main/src/main/java/uk/gov/defra/trade/imports/fish/be/exceptions/GlobalExceptionHandler.java) is a Spring `@RestControllerAdvice` that returns RFC 7807 ProblemDetail responses for the exceptions it handles. Framework-generated errors such as a missing route can use a different format.

### Handled Exceptions

#### Validation Errors (400 Bad Request)

Thrown when request body fails Bean Validation.

**Trigger**:

```bash
curl -X POST http://localhost:8085/example \
  -H "Content-Type: application/json" \
  -d '{"name":"","value":""}'  # Empty required fields
```

**Response**:

```json
{
  "type": "https://api.cdp.defra.cloud/problems/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed for one or more fields",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "errors": {
    "name": "Name is required",
    "value": "Value is required"
  }
}
```

**Validation Rules** (from `Example` entity):

- `name` — `@NotBlank` required
- `value` — `@NotBlank` required

#### Not Found (404 Not Found)

Triggered by `NotFoundException`.

**Response**:

```json
{
  "type": "https://api.cdp.defra.cloud/problems/not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Example not found with id: 507f1f77bcf86cd799439011",
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### Conflict (409 Conflict)

Triggered by `ConflictException`.

**Response**:

```json
{
  "type": "https://api.cdp.defra.cloud/problems/conflict",
  "title": "Resource Conflict",
  "status": 409,
  "detail": "Example with name 'duplicate' already exists",
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### Internal Server Error (500 Internal Server Error)

Triggered by unexpected runtime exceptions:

- `RuntimeException`
- `IllegalStateException`
- `IllegalArgumentException`

**Response**:

```json
{
  "type": "https://api.cdp.defra.cloud/problems/internal-error",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected error occurred. Please try again later.",
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Security Note**: Error detail does not expose sensitive information. Full exception and stack trace logged server-side with trace ID.

### Other Framework Exceptions

The exception handler does not catch every Spring framework exception:

- `NoResourceFoundException` — handled by Spring as 404
- `HttpMessageNotReadableException` — caught by the `RuntimeException` handler and returned as 500
- Other servlet container exceptions

Other framework errors may use Spring's built-in error handling, while malformed JSON is caught by the runtime exception handler.

## Trace ID Correlation

Custom error responses include `traceId` only when a non-blank trace ID was supplied in the request header; otherwise the property is omitted.

**Flow**:

1. Request arrives with a non-blank `x-cdp-request-id` header
2. `RequestTracingFilter` populates MDC with the trace ID; if the header is missing or blank, it remains unset
3. Exception occurs; `GlobalExceptionHandler` retrieves trace ID from MDC
4. Trace ID is included in the ProblemDetail response only when set
5. Logs include the trace ID in ECS format when supplied
6. Client uses trace ID to find request in logs

**Example workflow**:

```bash
# Request with trace ID
curl http://localhost:8085/example/invalid-id \
  -H "x-cdp-request-id: 550e8400-e29b-41d4-a716-446655440000"

# Response with same trace ID
# {
#   "traceId": "550e8400-e29b-41d4-a716-446655440000",
#   ...
# }

# Server logs (ECS format)
# {
#   "@timestamp": "2024-11-21T13:16:51Z",
#   "log.level": "WARN",
#   "log.logger": "uk.gov.defra.trade.imports.fish.be.exceptions.GlobalExceptionHandler",
#   "trace.id": "550e8400-e29b-41d4-a716-446655440000",
#   "message": "Resource not found..."
# }
```

## Logging Strategy

Exceptions are logged based on severity:

| Exception                         | Level | Logged By                                 |
| --------------------------------- | ----- | ----------------------------------------- |
| `MethodArgumentNotValidException` | WARN  | GlobalExceptionHandler                    |
| `NotFoundException`               | WARN  | Service + GlobalExceptionHandler          |
| `ConflictException`               | WARN  | Service + GlobalExceptionHandler          |
| `RuntimeException`                | ERROR | GlobalExceptionHandler (with stack trace) |

**Example logs**:

```
WARN  - Validation error (trace: 550e8400...): Failed to convert value
WARN  - Resource not found (trace: 550e8400...): Example not found with id: 507f...
WARN  - Resource conflict (trace: 550e8400...): Example with name 'dup' already exists
ERROR - Unexpected error (trace: 550e8400...): NullPointerException
  at uk.gov.defra.trade.imports.fish.be.service.ExampleService.create(...)
```

## Duplicate Key Handling (Race Condition Mitigation)

MongoDB's unique index on `name` can throw `DuplicateKeyException` if two requests try to create the same name simultaneously.

**Service handling** (in `ExampleService.create()`):

```java
try {
    Example saved = repository.save(entity);
} catch (DuplicateKeyException e) {
    throw new ConflictException("Example with name '" + entity.getName() + "' already exists");
}
```

This converts MongoDB's exception to our domain exception (409 Conflict).

## HTTP Status Code Reference

| Code  | Meaning               | Handler                                        |
| ----- | --------------------- | ---------------------------------------------- |
| `200` | OK                    | Successful GET/PUT                             |
| `201` | Created               | Successful POST                                |
| `204` | No Content            | Successful DELETE                              |
| `400` | Bad Request           | Validation failed                              |
| `404` | Not Found             | Resource doesn't exist                         |
| `409` | Conflict              | Name already exists                            |
| `500` | Internal Server Error | Unexpected exception, including malformed JSON |

## Troubleshooting

**"Example not found"** (404):

- Verify the example ID exists: `GET /example/{id}`
- Check ID is valid MongoDB ObjectId format (24 hex characters)
- Example may have been deleted

**"Example with name X already exists"** (409):

- Cannot create/rename to a name that's already in use
- Delete the conflicting example or use a different name
- If renaming to the same name, operation succeeds (idempotent)

**"Validation failed"** (400):

- Check required fields: `name` and `value` must be non-empty
- Verify JSON is valid
- Check field data types (all fields are strings except `counter` which is integer)

**"Internal Server Error"** (500):

- Check server logs with the trace ID from the response
- Verify MongoDB is accessible
- Check for resource exhaustion (memory, disk, connections)
