# Documentation Deployment Guide

## Overview

The `docs-deploy.yml` workflow automatically builds and deploys documentation to GitHub Pages when changes are pushed to the `main` branch. This ensures your project documentation is always up to date and publicly accessible.

## File Location

```
.github/workflows/docs-deploy.yml
```

## Documentation URL

**Published Site:** <https://defra.github.io/trade-imports-fish-be>

Deployed automatically to GitHub Pages when you push to `main`.

## Deployment Process

### How It Works

The workflow runs automatically with these steps:

1. **Trigger** - Push to `main` branch with changes in `docs/**`
2. **Checkout** - Retrieve repository code
3. **Setup Node.js** - Install Node.js 24.x
4. **Install uv** - Install Python package manager from pinned commit hash
5. **Install Dependencies** - Run `npm run install-deps` to set up MkDocs
6. **Build Docs** - Generate static site with `npm run build`
7. **Configure Pages** - Setup GitHub Pages
8. **Upload Artifact** - Store built site (`docs/site/`)
9. **Deploy** - Upload artifact to GitHub Pages (on `push` only, not PRs)

### Trigger Events

The workflow runs when:

✅ **Push to main** with changes in `docs/`

- Builds documentation
- Deploys to GitHub Pages

✅ **Pull Request to main** with changes in `docs/`

- Builds documentation for preview
- Does NOT deploy (checks only)

### Configuration

```yaml
name: Deploy Docs

on:
  pull_request:
    branches:
      - main
    paths:
      - 'docs/**'          # Only run on docs changes

  push:
    branches:
      - main
    paths:
      - 'docs/**'          # Only run on docs changes
```

## Workflow Stages

### Build Stage

Runs on both pull requests and pushes:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - Checkout code
      - Setup Node.js 24
      - Install uv (Python package manager)
      - npm run install-deps     # Install MkDocs + dependencies
      - npm run build            # Generate static site
      - Configure GitHub Pages
      - Upload artifact (docs/site/)
```

**Output:** Static HTML site in `docs/site/`

**Duration:** ~2-3 minutes

### Deploy Stage

Runs **only** on successful pushes to main (not on pull requests):

```yaml
jobs:
  deploy:
    if: github.event_name != 'pull_request'
    needs: build
    
    steps:
      - Checkout code
      - Deploy artifact to GitHub Pages
```

**Output:** Published to <https://defra.github.io/trade-imports-fish-be>

**Duration:** ~1 minute

## GitHub Pages Configuration

The site is configured as follows:

- **Domain:** `defra.github.io`
- **Path:** `/trade-imports-fish-be`
- **Source:** GitHub Actions (automated deployment)
- **Branch:** Deployed from actions/upload-pages-artifact

## Deployment Checks

### On Pull Request

✅ **Build succeeds** - Documentation can be built
❌ **Build fails** - Fix errors before merging

**What it checks:**

- Markdown syntax validity
- Link resolution
- MkDocs configuration
- Dependencies installation
- Asset availability

**What it does NOT do:**

- Deploy to live site (only on `push`)
- Publish to GitHub Pages

### On Push to Main

✅ **Build succeeds** - Build stage passes
✅ **Deploy succeeds** - Published to GitHub Pages

**Live URL:** <https://defra.github.io/trade-imports-fish-be>

**Visible:** Immediately after deployment (usually within 1-2 minutes)

## Common Scenarios

### Scenario 1: Documentation Change in Feature Branch

```bash
git checkout -b docs/my-update
# Edit docs/wiki/user-guide/api-usage.md
git commit -m "docs: update API usage guide"
git push origin docs/my-update
gh pr create --base main
```

**Result:**

- 🔨 Workflow runs: Build succeeds
- ✅ PR status: "Deploy Docs" shows as passing build
- 🔴 Deploy: Skipped (pull request, not push)
- 📝 Site: Not updated yet

### Scenario 2: Merge Documentation to Main

```bash
# PR is merged to main
# (or directly pushed to main)
```

**Result:**

- 🔨 Workflow runs: Build succeeds
- ✅ Build stage: Completes successfully
- 📤 Deploy stage: Runs (push event detected)
- ✅ Deploy: Artifact published to GitHub Pages
- 🌐 Live Site: Updated within 1-2 minutes

### Scenario 3: Documentation with Code Changes

```bash
git checkout -b feature/new-api-endpoint
# Update Java code for new endpoint
# Update docs/wiki/api/reference.md
git add .
git commit -m "feat: add new API endpoint and update docs"
git push origin feature/new-api-endpoint
gh pr create --base main
```

**Result:**

- 🔨 Workflow runs: Path filter matches (docs/ changed)
- ✅ PR status: Build succeeds
- 📝 When merged: Deployed to GitHub Pages

### Scenario 4: Code-Only Change (No Docs)

```bash
git checkout -b fix/internal-bug
# Update Java code only
# No changes to docs/
git commit -m "fix: internal bug"
git push origin fix/internal-bug
```

**Result:**

- ⏭️ Workflow skipped: No docs/ changes (path filter)
- 🚫 "Deploy Docs" does not run
- 📝 Site: Unchanged

## Workflow Dependencies

The deployment relies on these components:

| Component | Version | Purpose |
| ----------- | --------- | --------- |
| **Node.js** | 24.x | Task runner, linting |
| **uv** | Pinned commit | Fast Python package manager |
| **Python** | 3.11+ | MkDocs runtime |
| **MkDocs** | 1.6.1+ | Documentation generator |
| **Material for MkDocs** | 9.7.6+ | Theme |
| **mkdocs-awesome-pages-plugin** | 2.10.1+ | Navigation generation |

## Action Pinning

The workflow pins the `astral-sh/setup-uv` action to a specific commit hash:

```yaml
- name: Install uv
  uses: astral-sh/setup-uv@d0cc045d04ccac9d8b7881df0226f9e82c39688e
```

This provides:

✅ **Reproducibility** - Exact same uv version every run
✅ **Security** - Prevents action updates without review
✅ **Stability** - No breaking changes from action updates

To update the pinned hash, maintainers must:

1. Decide on new version
2. Find commit hash of that release
3. Update the hash in workflow
4. Create PR and review

## Troubleshooting

### Build Fails in Workflow

**Check these common issues:**

1. **Markdown syntax error**
   - Solution: Run `npm run lint:fix` locally before pushing

2. **Broken link**
   - Solution: Verify relative paths are correct (e.g., `../section/page.md`)

3. **Missing dependency**
   - Solution: Ensure `docs/pyproject.toml` lists required packages

4. **Asset not found**
   - Solution: Check image/file paths are correct and committed

### Deploy Not Happening

**Verify:**

1. ✅ Changes are in `docs/` directory
2. ✅ Pushed to `main` branch (not feature branch)
3. ✅ Build stage passed
4. ✅ GitHub Pages is enabled in repository settings

**Check GitHub Actions logs:**

1. Go to repository → Actions tab
2. Find "Deploy Docs" workflow
3. Click on latest run
4. View build/deploy job logs

### Site Not Updated

**Wait and refresh:**

- Deployment typically takes 1-2 minutes
- GitHub Pages has a slight cache delay
- Try hard refresh: `Cmd+Shift+R` (macOS) or `Ctrl+Shift+R` (Windows)

**Check deployment status:**

1. Repository → Settings → Pages
2. Look for "Your site is live at..." with deployment status
3. Click "last deployment" for details

## GitHub Pages Settings

Verify repository GitHub Pages configuration:

1. Go to repository → Settings → Pages
2. **Source:** Select "GitHub Actions"
3. **Branch:** N/A (automated deployment)
4. **Custom domain:** Optional (currently using default)

## Deployment Timeline

| Action | Timing |
| -------- | -------- |
| Push to main | Immediate |
| Workflow starts | Within 30 seconds |
| Build stage | ~2-3 minutes |
| Deploy stage | ~1 minute |
| Live site updated | 1-2 minutes after deploy completes |
| **Total time** | ~5 minutes |

## Monitoring Deployments

### GitHub Actions Dashboard

```
Repository → Actions → Deploy Docs
```

Shows:

- ✅ Workflow status (success/failure)
- 📊 Execution duration
- 🔍 Build and deploy logs
- 📄 Artifact upload status

### GitHub Pages Dashboard

```
Repository → Settings → Pages
```

Shows:

- 🌐 Live site URL
- 📍 Last deployment status
- 📅 Last deployment timestamp
- 🚀 Deployment history

## Best Practices

### ✅ Do's

- **Build locally before pushing** - Run `npm run build` to catch errors
- **Lint documentation** - Run `npm run lint:fix` before committing
- **Test links locally** - Use `npm run dev` to verify navigation
- **Review changes in PR** - Check build status before merging
- **Keep docs in sync** - Use [docs-update-checker](docs-update-workflow.md) workflow
- **Document changes clearly** - Write meaningful commit messages

### ❌ Don'ts

- **Don't commit broken docs** - Fix before pushing
- **Don't ignore workflow failures** - Address errors before merging
- **Don't manually edit GitHub Pages** - Only edit docs/ and push
- **Don't remove docs/ paths from trigger** - Needed for efficiency
- **Don't modify action pins without review** - Security/stability risk
- **Don't edit site/ directory** - It's auto-generated and ignored

## Related Workflows

This project has two documentation workflows:

| Workflow | Purpose | Trigger |
|----------|---------|---------|
| **docs-deploy.yml** | Build and deploy to GitHub Pages | Push/PR to main with docs/ changes |
| **docs-update-checker.yml** | Validate PRs include doc updates | PR events on main branch |

See [Documentation Update Checker](docs-update-workflow.md) for PR validation.

## Related Pages

- [Documentation Development Setup](../getting-started/setup.md) - Local development setup
- [Documentation Update Checker](docs-update-workflow.md) - PR validation
- [Contributing Guide](guide.md) - Contribution process
- [Pull Request Process](pull-requests.md) - PR guidelines
- [Getting Started: Running](../getting-started/running.md) - Local app setup

## Support

For deployment issues:

1. Check this guide first
2. Review workflow logs in GitHub Actions
3. Verify local build: `cd docs && npm run build`
4. Check GitHub Pages settings
5. Ask in team chat or create an issue

---

**Last Updated:** November 18, 2026  
**Workflow File:** `.github/workflows/docs-deploy.yml`  
**Published Site:** <https://defra.github.io/trade-imports-fish-be>  
**Deployment:** Automated via GitHub Actions
