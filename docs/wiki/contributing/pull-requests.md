# Pull Request Process

## Creating a Pull Request

### 1. Ensure Your Changes Are Ready

- [ ] Code is complete and tested
- [ ] Tests pass: `mvn clean verify`
- [ ] Code follows standards
- [ ] Documentation is updated
- [ ] Commit messages are clear

### 2. Push Your Branch

```bash
git push origin feature/your-feature-name
```

### 3. Open a Pull Request

Go to GitHub and open a PR from your feature branch to the main branch.

## PR Title and Description

### Title Format

Use a clear, descriptive title:

- ✅ `Add validation for import user data`
- ✅ `Fix MongoDB connection timeout issue`
- ❌ `Update code`
- ❌ `WIP`

### Description Template

```markdown
## Description

Brief description of your changes.

## Type of Change

- [ ] Bug fix
- [ ] New feature
- [ ] Documentation update
- [ ] Performance improvement

## Related Issues

Closes #123

## Changes Made

- Change 1
- Change 2
- Change 3

## Testing

Describe how you tested your changes:

- [ ] Unit tests written
- [ ] Integration tests written
- [ ] Manual testing performed

## Screenshots (if applicable)

Add screenshots for UI changes.

## Checklist

- [ ] Code follows project standards
- [ ] Tests are included
- [ ] Documentation is updated
- [ ] Commit messages are clear
- [ ] No breaking changes
```

## Review Process

### Expectations

- **Response Time**: Reviews within 2-3 days
- **Constructive Feedback**: Focus on code quality
- **Respect**: Professional and courteous communication

### Addressing Feedback

1. Make requested changes
2. Push new commits to the same branch
3. Request re-review
4. Respond to comments explaining changes

### Approval

- At least one maintainer approval required
- All checks must pass (tests, linting)
- No unresolved conversations

## Merging

### By Maintainers

- Squash and merge (for feature branches)
- Create and merge (for larger PRs)
- Delete branch after merging

### Branch Protection Rules

- Require PR review
- Require status checks to pass
- Require branches to be up to date

## Common Mistakes

❌ **Don't:**
- Commit sensitive data (passwords, API keys)
- Make unrelated changes in one PR
- Ignore test failures
- Skip documentation
- Force push after opening PR

✅ **Do:**
- Keep PRs focused and reasonably sized
- Write clear commit messages
- Include tests with your changes
- Update relevant documentation
- Respond to reviewer feedback promptly

## PR Size Guidelines

| Size | Max Files | Max Lines | Scope |
|------|-----------|-----------|-------|
| Small | 3-5 | 100-200 | Single feature or fix |
| Medium | 5-10 | 200-500 | Multi-file feature |
| Large | 10+ | 500+ | Major feature (split if possible) |

## Next Steps

- [Contributing Guide](guide.md)
- [Code Standards](standards.md)
