# Documentation Update Checker Workflow Guide

## Overview

The `docs-update-checker.yml` workflow is a GitHub Actions pipeline that automatically validates that pull requests to the `main` branch include documentation updates when applicable. This ensures your codebase documentation stays in sync with code changes.

## File Location

```
.github/workflows/docs-update-checker.yml
```

## How It Works

### Trigger Events

The workflow runs automatically on pull requests to the `main` branch when:

- 🔓 Pull request is opened
- 🏷️ Label is added to PR
- 🏷️ Label is removed from PR  
- 📝 New commits are pushed (synchronize)
- 🔄 Pull request is reopened

### Workflow Logic

```
1. Check for 'no-docs' label
   ├─ If label present → SKIP check
   └─ If label absent → CONTINUE

2. Fetch main branch for comparison

3. Compare PR branch vs main branch

4. Check docs/wiki directory for changes
   ├─ If changes found → ✅ PASS
   └─ If NO changes → ❌ FAIL with message
```

## Documentation Directories

The workflow checks for changes in any of these directories:

```
docs/wiki/
├── getting-started/       # Setup and initial configuration
│   ├── index.md
│   ├── setup.md
│   └── running.md
├── user-guide/            # Usage and features
│   ├── index.md
│   ├── api-usage.md
│   ├── database.md
│   ├── authentication.md
│   ├── configuration.md
│   └── troubleshooting.md
├── api/                   # API reference
│   ├── index.md
│   ├── reference.md
│   ├── authentication.md
│   └── error-handling.md
├── architecture/          # System design
│   ├── index.md
│   └── configuration.md
├── configuration/         # Configuration guide
│   └── index.md
└── contributing/          # Contribution guidelines
    ├── index.md
    ├── guide.md
    ├── pull-requests.md
    ├── standards.md
    └── quality/
        └── index.md
```

## Using the 'no-docs' Label

### When to Add 'no-docs' Label

Add this label to your PR if your changes **do not** require documentation updates:

✅ **Good use cases:**

- Internal code refactoring (no user impact)
- Bug fixes in internal utilities
- Test additions or improvements
- CI/CD pipeline changes
- Dependency version updates (non-breaking)
- Performance optimizations (no API changes)
- Code cleanup or formatting

❌ **Do NOT use 'no-docs' for:**

- New user-facing features
- API endpoint changes
- Configuration option changes
- Breaking changes
- New authentication methods
- Database schema changes
- Deployment procedure changes

### How to Add the Label

**Via GitHub UI:**

1. Open your PR
2. Click "Labels" on the right sidebar
3. Search for "no-docs"
4. Click to add the label
5. The workflow will automatically skip the check

**Via GitHub CLI:**

```bash
gh pr edit <PR_NUMBER> --add-label "no-docs"
```

## Error Messages & Responses

### ❌ When Check Fails

```
Error: No changes found in docs/wiki directory.

📝 Please update the documentation to reflect your changes in this PR.

ℹ️  To bypass this check, add the 'no-docs' label to your PR if:
  • Your changes don't affect user-facing functionality
  • This is a bug fix or internal refactoring
  • Documentation updates are not necessary

📚 Documentation location: docs/wiki/
   - Getting Started: docs/wiki/getting-started/
   - User Guide: docs/wiki/user-guide/
   - API Reference: docs/wiki/api/
   - Architecture: docs/wiki/architecture/
   - Configuration: docs/wiki/configuration/
   - Contributing: docs/wiki/contributing/
```

**How to fix:**

1. Update relevant documentation files in `docs/wiki/`
2. Commit and push changes
3. Workflow will automatically re-run

### ✅ When Check Passes

```
Documentation changes found in docs/wiki/

The following files were updated:
docs/wiki/user-guide/api-usage.md
docs/wiki/configuration/index.md

Thank you for keeping the documentation up to date! 🎉
```

## Workflow Status

### Checking Status in GitHub

1. Open your pull request
2. Scroll to the bottom
3. Look for "Check Documentation Updates" status
4. Shows either ✅ PASS or ❌ FAIL

### GitHub Actions Tab

View full details:

1. Go to repository → Actions tab
2. Look for "Documentation Update Checker"
3. Click on your PR run
4. See full output and details

## Examples

### Example 1: Adding New Feature with Docs

```bash
# Create feature branch
git checkout -b feature/new-api-endpoint

# Make code changes
# ... code changes ...

# Update documentation
echo "## New Endpoint" >> docs/wiki/api/reference.md

# Commit and push
git add .
git commit -m "feat: add new API endpoint"
git push origin feature/new-api-endpoint

# Create PR
gh pr create --base main --head feature/new-api-endpoint
```

✅ Workflow will **PASS** because docs/wiki changed

---

### Example 2: Bug Fix Without Doc Changes

```bash
# Create fix branch
git checkout -b fix/internal-bug

# Make code changes
# ... bug fix ...

# Commit and push
git add .
git commit -m "fix: internal bug in utilities"
git push origin fix/internal-bug

# Create PR
gh pr create --base main --head fix/internal-bug

# Add label to skip check
gh pr edit <PR_NUMBER> --add-label "no-docs"
```

✅ Workflow will **SKIP** because of 'no-docs' label

---

### Example 3: Configuration Change with Docs

```bash
# Create feature branch
git checkout -b feature/new-config-option

# Make code changes for new config option
# ... code changes ...

# Update configuration documentation
echo "## New Config Option" >> docs/wiki/configuration/index.md

# Commit and push
git add .
git commit -m "feat: add new configuration option"
git push origin feature/new-config-option

# Create PR
gh pr create --base main --head feature/new-config-option
```

✅ Workflow will **PASS** because docs/wiki changed

---

### Example 4: Dependency Update

```bash
# Create update branch
git checkout -b chore/update-dependencies

# Update dependencies
# ... package/pom updates ...

# Commit and push
git add .
git commit -m "chore: update dependencies"
git push origin chore/update-dependencies

# Create PR
gh pr create --base main --head chore/update-dependencies

# Add label because no docs needed for dependency updates
gh pr edit <PR_NUMBER> --add-label "no-docs"
```

✅ Workflow will **SKIP** because of 'no-docs' label

## Workflow Configuration Details

```yaml
name: Documentation Update Checker

on:
  pull_request:
    types:
      - opened              # When PR is created
      - labeled             # When label is added
      - unlabeled           # When label is removed
      - synchronize         # When commits are pushed
      - reopened            # When PR is reopened
    branches:
      - main                # Only for main branch

jobs:
  check-docs-commit:
    name: Check Documentation Updates
    if: >-
      !contains(github.event.pull_request.labels.*.name, 'no-docs')
    # Skips if 'no-docs' label is present
    
    runs-on: ubuntu-latest
    
    steps:
      - Checkout code
      - Fetch main branch
      - Compare PR vs main for docs/wiki changes
```

## Best Practices

### ✅ Do's

- **Update docs with code changes** - Keep documentation synchronized
- **Use descriptive commit messages** - Help reviewers understand changes
- **Review documentation** - Ensure accuracy and clarity
- **Use 'no-docs' label honestly** - Only when truly not needed
- **Ask questions** - If unsure, ask maintainers

### ❌ Don'ts

- **Don't bypass with 'no-docs' unnecessarily** - Defeats the purpose
- **Don't let documentation rot** - Outdated docs are worse than no docs
- **Don't rush documentation** - Take time to write clear docs
- **Don't ignore the workflow** - Address it before merging
- **Don't delete old docs** - Archive if needed, don't remove

## FAQ

### Q: Can I merge if the check fails?

**A:** No, the check must pass before merging. You must either:

1. Update documentation, OR
2. Add 'no-docs' label if appropriate

### Q: How do I update documentation?

**A:** Edit Markdown files in `docs/wiki/`. Refer to [MkDocs documentation setup guide](https://github.com/DEFRA/trade-imports-fish-be/blob/main/docs/README.md) for guidelines on:

- File structure
- Adding new pages
- Markdown features
- Linting documentation

### Q: What if my PR has no code changes?

**A:** If you're only updating documentation, the check still passes as long as docs/wiki changed.

### Q: Can I remove the 'no-docs' label later?

**A:** Yes, removing the label will trigger the workflow again. If you added docs since creating the PR, it should pass.

### Q: Does the workflow check docs/README.md?

**A:** No, it only checks `docs/wiki/`. The README.md in docs/ directory is separate.

### Q: What if docs/wiki is empty?

**A:** The workflow will still pass if ANY file in docs/wiki is added, modified, or deleted.

### Q: How long does the workflow take?

**A:** Typically 10-30 seconds, depending on GitHub's runner availability.

## Troubleshooting

### Workflow shows error but I updated docs

**Solution:**

1. Verify files are in `docs/wiki/` directory
2. Ensure files are committed (not just staged)
3. Push commits to your branch
4. Wait for workflow to re-run (usually 30 seconds)
5. Check "Check Documentation Updates" status

### Can't find 'no-docs' label

**Solution:**

1. Labels might need to be created first
2. Ask a maintainer to create the label
3. Or use the GitHub CLI:

   ```bash
   gh label create "no-docs" -d "Skip documentation check"
   ```

### Workflow stuck or not running

**Solution:**

1. Close and reopen the PR
2. Push an empty commit: `git commit --allow-empty -m "re-trigger workflow"`
3. Check GitHub Actions tab for full details

## Related Workflows

This project has two documentation-related workflows:

1. **docs-deploy.yml** - Builds and deploys docs to GitHub Pages
2. **docs-update-checker.yml** - Validates PRs have doc updates (this file)

## Support

For questions or issues:

1. Check this guide first
2. Review the example scenarios above
3. Ask in PR comments or team chat
4. Reach out to documentation maintainers

---

**Last Updated:** November 6, 2026  
**Workflow File:** `.github/workflows/docs-update-checker.yml`  
**Documentation:** `docs/wiki/`  
**Main Branch:** `main`
