# Testing

## Overview

The Trade Imports Fish Backend has comprehensive test coverage to ensure quality and reliability.

## Test Types

### 1. Unit Tests

Test individual components in isolation.

**Location**: `src/test/java/**/*Test.java`

**Run**:
```bash
mvn test
```

**Example**:
```java
@Test
void should_validateUser_when_validData() {
    User user = new User("John", "john@example.com");
    assertTrue(validator.validateUser(user));
}
```

### 2. Integration Tests

Test multiple components working together.

**Location**: `src/test/java/**/*IT.java`

**Run**:
```bash
mvn verify
```

**Uses TestContainers** for containerized MongoDB.

### 3. End-to-End Tests

Test complete application workflows.

**Run**:
```bash
mvn clean verify -DskipITs=false
```

## Running Tests

### All Tests

```bash
mvn clean verify
```

### Unit Tests Only

```bash
mvn test
```

### Integration Tests Only

```bash
mvn integration-test
```

### Specific Test Class

```bash
mvn test -Dtest=UserValidatorTest
```

### Specific Test Method

```bash
mvn test -Dtest=UserValidatorTest#testValidUser
```

## Code Coverage

Generate coverage report:

```bash
mvn clean verify jacoco:report
```

View report: `target/site/jacoco/index.html`

**Coverage Goals**:
- Overall: >80%
- Core logic: >85%
- Controllers: >75%

## Test Structure

### Arrange-Act-Assert Pattern

```java
@Test
void should_createUser_when_validData() {
    // Arrange
    CreateUserRequest request = new CreateUserRequest("John", "john@example.com");
    
    // Act
    User user = userService.createUser(request);
    
    // Assert
    assertNotNull(user.getId());
    assertEquals("john@example.com", user.getEmail());
}
```

## Testing Best Practices

### 1. Use Descriptive Names

```java
// Good
void should_returnUser_when_validIdProvided()
void should_throwException_when_invalidEmail()

// Poor
void testUser()
void test1()
```

### 2. Test One Thing

```java
// Good: Tests one behavior
@Test
void should_validateEmail_when_invalidFormat() {
    assertFalse(validator.isValidEmail("invalid"));
}

// Poor: Tests multiple behaviors
@Test
void should_validateUser() {
    assertFalse(validator.isValidEmail("invalid"));
    assertFalse(validator.isValidName(""));
    assertTrue(validator.isValidAge(25));
}
```

### 3. Use Test Fixtures

```java
@BeforeEach
void setUp() {
    userRepository.save(new User("John", "john@example.com"));
}

@AfterEach
void tearDown() {
    userRepository.deleteAll();
}
```

### 4. Mock External Dependencies

```java
@Mock
private UserRepository repository;

@InjectMocks
private UserService service;

@Test
void should_getUserById_when_found() {
    // Arrange
    User user = new User("John", "john@example.com");
    when(repository.findById("1")).thenReturn(Optional.of(user));
    
    // Act
    User result = service.getUserById("1");
    
    // Assert
    assertEquals(user.getEmail(), result.getEmail());
}
```

## Common Testing Issues

### Issue: Tests Fail Locally But Pass in CI

**Solution**: Ensure test data is isolated and doesn't depend on execution order.

### Issue: Integration Tests Are Slow

**Solution**: Use `@DataMongoTest` to test only data layer, or run in parallel:
```bash
mvn test -DuseUnlimitedThreads=true
```

### Issue: MongoDB Connection Timeouts

**Solution**: Ensure MongoDB is running and accessible:
```bash
docker ps | grep mongodb
```

## Next Steps

- [Code Standards](../standards.md)
- [Contributing Guide](../guide.md)
