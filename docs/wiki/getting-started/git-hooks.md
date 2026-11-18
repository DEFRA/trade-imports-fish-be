# Git Hooks Setup

This guide covers configuring Git hooks for automatic documentation updates during
development.

## Overview

The repository uses Git hooks to automatically update technical documentation when
code changes are committed. The pre-commit hook runs the `update-docs.sh` script,
which uses the Copilot CLI to keep documentation in sync with implementation
changes.

## Prerequisites

Before setting up Git hooks, ensure you have:

- **Copilot CLI** - Install from
  [github.com/github/gh-copilot](https://github.com/github/gh-copilot)
- **GitHub token** - Fine-grained Personal Access Token with the account-level **Copilot Requests** permission
- **Git** - 2.9+ (supports custom hooks directory)

## Step 1: Configure Git Hooks Directory

Tell Git to use the `.githooks` directory for custom hooks:

```bash
git config core.hooksPath .githooks
```

> **Note:** This is a per-repository setting. Each team member must run this
> command after cloning. This setting is NOT shared via git configuration.

Verify the configuration:

```bash
git config core.hooksPath
# Output: .githooks
```

## Step 2: Make Pre-Commit Hook Executable

The pre-commit hook requires execute permissions:

```bash
chmod +x .githooks/pre-commit
```

Verify:

```bash
ls -l .githooks/pre-commit
# Should show: -rwxr-xr-x (or similar with 'x' execute permission)
```

## Step 3: Set Up GitHub Token

The pre-commit hook uses the Copilot CLI to update documentation, which requires
GitHub authentication.

### Create a Personal Access Token

1. Go to [fine-grained personal access tokens](https://github.com/settings/personal-access-tokens/new)
2. Create a new fine-grained personal access token
3. Give it a descriptive name (e.g., `copilot-docs-update`)
4. Set an appropriate expiration
5. Under account permissions, enable **Copilot Requests**
6. Generate the token
7. Copy it immediately and export it using one of the environment variables below

### Add Token to Your Environment

Choose one method:

## Option A: Shell Profile (Recommended)

Add to `~/.bashrc`, `~/.zshrc`, or `~/.bash_profile`:

```bash
export GITHUB_TOKEN=your_token_here
```

Reload your shell:

```bash
source ~/.bashrc   # or ~/.zshrc for zsh
```

## Option B: Copilot-Specific Environment Variable

Export `COPILOT_GITHUB_TOKEN=your_token_here` in the shell that will run Git, or add that export to your shell profile.

## Option C: Temporary (Current Session Only)

```bash
export GITHUB_TOKEN=your_token_here
```

### Supported Environment Variables

The pre-commit hook looks for these in order:

1. `COPILOT_GITHUB_TOKEN` - Copilot CLI specific
2. `GH_TOKEN` - GitHub CLI token
3. `GITHUB_TOKEN` - Standard GitHub token

Use only one to avoid confusion.

## Step 4: Verify Setup

Test your configuration before committing code:

```bash
# Check git hooks directory is configured
git config core.hooksPath

# Check pre-commit hook is executable
ls -l .githooks/pre-commit

# Check GitHub token is set
echo $GITHUB_TOKEN   # Should show your token (not empty)

# Or verify git config token
git config credential.https://github.com.token
```

If any step fails, see [Troubleshooting](#troubleshooting).

## How It Works

When you commit code, the pre-commit hook:

1. **Detects staged changes** - Identifies code modifications (not docs)
2. **Runs update-docs.sh** - Calls the documentation update script
3. **Updates docs/** - Uses Copilot CLI to update relevant documentation
4. **Stages changes** - Adds updated docs to your commit automatically
5. **Completes commit** - Proceeds with your commit if successful

If only documentation changed, the hook skips the update (no code changes to
reflect).

### Example Flow

```bash
# Edit Java code
vim src/main/java/Example.java

# Stage the change
git add src/main/java/Example.java

# Commit (pre-commit hook runs automatically)
git commit -m "feat: add example feature"

# Output:
# update-docs: updating docs/ for staged changes
# update-docs: docs/ updated and staged
# [feature/my-feature 1a2b3c4] feat: add example feature
#  2 files changed, ...
```

Notice that two files changed: your code + updated documentation.

## Bypassing the Hook

Skip the pre-commit hook when needed:

```bash
git commit --no-verify -m "your message"
```

**Use sparingly.** Skipping may cause documentation to become out of sync.

## Troubleshooting

### Error: "copilot CLI is required"

**Cause:** Copilot CLI is not installed.

**Solution:**

Install [GitHub Copilot CLI](https://github.com/github/copilot-cli):

~~~bash
# macOS (Homebrew)
brew install copilot-cli

# Or via npm
npm install -g @github/copilot
~~~

Verify:

```bash
copilot --version
```

### Error: "set COPILOT_GITHUB_TOKEN, GH_TOKEN, or GITHUB_TOKEN"

**Cause:** No GitHub token environment variable is set.

**Solution:** Follow [Step 3: Set Up GitHub Token](#step-3-set-up-github-token)
above.

Verify:

```bash
# Check which variable is set
echo $COPILOT_GITHUB_TOKEN
echo $GH_TOKEN
echo $GITHUB_TOKEN
# At least one should output your token (not empty)
```

### Error: "unstaged tracked changes"

**Cause:** You have uncommitted changes outside of docs.

**Solution:** Commit or stash unstaged changes before committing:

```bash
# Stash temporary changes
git stash

# Or commit them
git add .
git commit -m "your message"
```

### Hook doesn't run on commit

**Cause:** Git hooks directory not configured or permissions issue.

**Solution:**

```bash
# Reconfigure hooks directory
git config core.hooksPath .githooks

# Make hook executable
chmod +x .githooks/pre-commit

# Verify
git config core.hooksPath
ls -l .githooks/pre-commit
```

### "docs/ not in correct state"

**Cause:** Docs have uncommitted changes.

**Solution:** Commit or stash docs changes:

```bash
# Stash docs changes
git stash -- docs/

# Or commit them
git add docs/
git commit -m "docs: update documentation"
```

### Token expired

**Cause:** Personal Access Token has expired.

**Solution:**

1. Create new token at [github.com/settings/tokens](https://github.com/settings/tokens)
2. Update your environment variable
3. Retry commit

## Related Pages

- [Development Setup](setup.md) - Local development environment
- [Running the Application](running.md) - How to run the backend
- [Contributing Guide](../contributing/guide.md) - Contribution process
- [Documentation Deployment](../contributing/docs-deploy.md) - How docs are deployed

## Next Steps

After setting up Git hooks:

1. [Set up your development environment](setup.md)
2. [Run the application locally](running.md)
3. [Read the contributing guide](../contributing/guide.md)
4. [Start making changes!](../contributing/guide.md)
