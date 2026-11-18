# Contributing Guide

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Follow project guidelines

## Getting Started

1. Fork the repository: <https://github.com/DEFRA/trade-imports-fish-be>
2. Clone your fork: `git clone https://github.com/your-username/trade-imports-fish-be.git`
3. Add upstream: `git remote add upstream https://github.com/DEFRA/trade-imports-fish-be.git`
4. [Setup development environment](../getting-started/setup.md)

## Development Workflow

### Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
```

Use descriptive branch names:

- `feature/add-user-validation`
- `fix/mongodb-connection-issue`
- `docs/api-documentation`

### Make Your Changes

1. Write clean, testable code
2. Follow [Code Standards](standards.md)
3. Write or update tests
4. Update documentation if needed

### Run Tests

```bash
mvn test
```

Run integration tests:

```bash
mvn clean verify
```

### Commit Your Changes

Use clear, descriptive commit messages:

```bash
git commit -m "Add user validation for imports"
```

Good commit message format:

- First line: short summary (50 chars or less)
- Blank line
- Detailed explanation (if needed)

Example:

```
Add validation for import user data

- Add UserValidator class
- Add unit tests for validation logic
- Update API documentation
```

### Push to Your Fork

```bash
git push origin feature/your-feature-name
```

## Pull Request Process

See [Pull Request Process](pull-requests.md) for detailed instructions.

## Code Review

Code reviews ensure quality:

1. **Completeness**: Does it fully address the issue?
2. **Testing**: Are there adequate tests?
3. **Documentation**: Is documentation updated?
4. **Standards**: Does it follow code standards?
5. **Performance**: Are there performance considerations?

## Common Issues

### Build Fails

1. Ensure Java 25 is installed
2. Run `mvn clean install`
3. Check Maven configuration

### Tests Fail

1. Ensure MongoDB is running
2. Check test configuration
3. Review test logs

### Linting Issues

Follow the code standards and run:

```bash
mvn checkstyle:check
```

Fix issues with:

```bash
mvn checkstyle:checkstyle
```

## Next Steps

- [Pull Request Process](pull-requests.md)
- [Code Standards](standards.md)
- [Testing Guide](quality/index.md)
