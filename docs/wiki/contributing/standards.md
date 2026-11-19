# Code Standards

## Coding Conventions

### Java Style

- **Formatting**: Follow Google Java Style Guide
- **Line Length**: Max 120 characters
- **Indentation**: 4 spaces
- **Naming**:
  - Classes: PascalCase
  - Methods: camelCase
  - Constants: UPPER_SNAKE_CASE
  - Variables: camelCase

### Example

```java
public class UserValidator {
    private static final int MAX_NAME_LENGTH = 100;
    private final UserRepository userRepository;
    
    public UserValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public boolean validateUser(User user) {
        if (user.getName().length() > MAX_NAME_LENGTH) {
            return false;
        }
        return true;
    }
}
```

## Comments and Documentation

- **Write self-documenting code**: Use clear names
- **Comment "why", not "what"**: Explain reasoning
- **JavaDoc**: Document public methods
- **TODO comments**: Include context and date

### Good Example

```java
/**
 * Validates user import data against business rules.
 * 
 * @param user the user to validate
 * @return true if valid, false otherwise
 * @throws IllegalArgumentException if user is null
 */
public boolean validateUser(User user) {
    // Check max length to prevent database overflow
    if (user.getName().length() > MAX_NAME_LENGTH) {
        return false;
    }
    return true;
}
```

## Testing Standards

### Unit Tests

- Test one thing per test
- Use descriptive names: `should_validateUser_when_validData()`
- Follow Arrange-Act-Assert pattern
- Aim for >80% code coverage

### Example

```java
@Test
void should_validateUser_when_validData() {
    // Arrange
    User user = new User("John", "john@example.com");
    
    // Act
    boolean result = validator.validateUser(user);
    
    // Assert
    assertTrue(result);
}
```

### Integration Tests

- Test complete workflows
- Use TestContainers for MongoDB
- Clean up after tests
- Name: `*IT.java`

## Error Handling

### Exception Types

```java
// For invalid input
throw new IllegalArgumentException("User name cannot be empty");

// For business rule violations
throw new BusinessRuleException("User already exists");

// For not found scenarios
throw new EntityNotFoundException("User not found with id: " + id);
```

### Global Exception Handler

Use `@ControllerAdvice` for consistent error responses:

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            EntityNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

## Spring Boot Best Practices

### Dependency Injection

```java
// Good: Constructor injection
@Service
public class UserService {
    private final UserRepository repository;
    
    public UserService(UserRepository repository) {
        this.repository = repository;
    }
}

// Avoid: Field injection
@Service
public class UserService {
    @Autowired
    private UserRepository repository;  // Harder to test
}
```

### Configuration

- Use `@ConfigurationProperties` for grouped properties
- Use `@Value` for individual properties
- Externalize configuration

### Logging

```java
private static final Logger log = LoggerFactory.getLogger(UserService.class);

public void createUser(User user) {
    log.info("Creating user: {}", user.getEmail());
    // ...
    log.debug("User created with id: {}", user.getId());
}
```

## Security

- **Validate input**: Always validate user input
- **SQL Injection**: Use parameterized queries (Spring Data handles this)
- **Authentication**: Protect sensitive endpoints
- **Encryption**: Encrypt sensitive data
- **Secrets**: Never commit secrets to version control

## Performance

- **Indexing**: Create indexes for frequently queried fields
- **Lazy Loading**: Use lazy loading for associations
- **Caching**: Cache frequently accessed data
- **Batch Operations**: Use bulk operations for large datasets

## Code Review Checklist

- [ ] Code is clean and readable
- [ ] No duplication
- [ ] Tests are included
- [ ] Error handling is appropriate
- [ ] Security best practices followed
- [ ] Performance considered
- [ ] Documentation updated
- [ ] Follows code standards

## Next Steps

- [Contributing Guide](guide.md)
- [Testing Guide](quality/index.md)
