# MkDocs Documentation Setup Guide

Complete guide for setting up, developing, and deploying MkDocs documentation for the Trade Imports Fish Backend project.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Prerequisites](#prerequisites)
3. [Git Hooks Setup](#git-hooks-setup)
4. [Installation](#installation)
5. [Running Development Server](#running-development-server)
6. [Available Commands](#available-commands)
7. [Project Structure](#project-structure)
8. [Configuration](#configuration)
9. [Development Workflow](#development-workflow)
10. [Building & Deployment](#building--deployment)
11. [Troubleshooting](#troubleshooting)
12. [Features & Technology Stack](#features--technology-stack)

---

## Quick Start

For the fastest setup:

```bash
cd docs
npm install                 # Install npm dependencies
brew install uv             # Install uv (or use alternative below)
npm run install-deps        # Install Python + MkDocs
npm run dev                 # Start development server
```

Then open your browser to: **<http://localhost:8000>**

**To stop the server:** Press `Ctrl+C` in your terminal.

---

## Prerequisites

Before starting, ensure you have the following installed:

- **Node.js 24.x** - [Download](https://nodejs.org/)
- **Python 3.11+** - [Download](https://www.python.org/)
- **uv (Python package manager)** - See [Installation Step 2](#step-2-install-uv-python-package-manager)

Verify installations:

```bash
node --version      # Should show: v24.x
npm --version       # Should show: 10.x or higher
python3 --version   # Should show: Python 3.11 or higher
uv --version        # Should show: uv 0.13.0 or higher
```

---

## Git Hooks Setup

This repository uses Git hooks to keep documentation in sync with code changes. Configure Git and make the pre-commit hook executable:

### Step 1: Configure Git Hooks Directory

Tell Git to use the `.githooks` directory instead of the default `.git/hooks`:

```bash
git config core.hooksPath .githooks
```

> **Note:** This is a per-repository setting. Each team member must run this command after cloning.

### Step 2: Make Pre-Commit Hook Executable

The pre-commit hook requires execute permissions:

```bash
chmod +x .githooks/pre-commit
```

### Step 3: Set Up GitHub Token

The pre-commit hook uses the Copilot CLI to update documentation, which requires GitHub authentication. You need to set up a Personal Access Token.

#### Create a Personal Access Token

1. Go to [github.com/settings/tokens?type=beta](https://github.com/settings/tokens?type=beta) to create a fine-grained Personal Access Token
2. Click **Generate new token**
3. Give it a descriptive name (e.g., `copilot-docs-update`)
4. Set the expiration (we recommend 90 days for security)
5. Under **Repository access**, select **Only select repositories** and choose this repository
6. Under **Permissions**, find **Copilot Requests** and set it to **Read and write**
7. Click **Generate token**
8. **Copy the token immediately** — you won't be able to see it again

#### Add the Token to Your Environment

Choose one of these methods:

**Option A: Shell Profile (Recommended for persistent setup)**

Add the token to your shell's profile file (`~/.bashrc`, `~/.zshrc`, or `~/.bash_profile`):

```bash
export GITHUB_TOKEN=your_token_here
```

Then reload your shell:

```bash
source ~/.bashrc   # or ~/.zshrc for zsh
```

**Option B: Git Configuration (Repository-specific)**

Store it in your git config:

```bash
git config --global credential.https://github.com.token your_token_here
```

**Option C: Environment Variable for One Session**

Export it temporarily in your current terminal:

```bash
export GITHUB_TOKEN=your_token_here
```

#### Supported Environment Variables

The pre-commit hook looks for tokens in this order:

1. `COPILOT_GITHUB_TOKEN` — explicitly for Copilot CLI
2. `GH_TOKEN` — GitHub CLI token
3. `GITHUB_TOKEN` — standard GitHub token

Use whichever you prefer; set only one to avoid confusion.

#### Troubleshooting Token Issues

- **"update-docs: set COPILOT_GITHUB_TOKEN, GH_TOKEN, or GITHUB_TOKEN..."**: One of the three token environment variables is not set. Follow the setup steps above.
- **"update-docs: copilot CLI is required..."**: The Copilot CLI is not installed. Install it from [github.com/github/gh-copilot](https://github.com/github/gh-copilot).
- **Token expired**: Regenerate a new fine-grained token at [github.com/settings/tokens?type=beta](https://github.com/settings/tokens?type=beta) and update your environment.
- **Authentication fails**: Ensure the token is a **fine-grained Personal Access Token** with **Copilot Requests** permission set to **Read and write**, and that your GitHub account has access to this repository.

### What the Pre-Commit Hook Does

When you commit code, the pre-commit hook automatically:

1. **Updates documentation** for any code changes (using Copilot CLI)
2. **Stages updated docs** in git if changes were made

This ensures documentation stays in sync with code changes.

### Bypassing the Hook

To skip the pre-commit hook when committing, use the `--no-verify` flag:

```bash
git commit --no-verify
```

> **Note:** Use this sparingly. Skipping the hook may cause documentation to become out of sync with code changes.

---

## Installation

### Step 1: Navigate to Docs Directory

```bash
cd docs
```

### Step 2: Install uv (Python Package Manager)

**Important:** `uv` is NOT an npm package—it's a standalone Python package manager. Install it using one of these methods:

**Option 1: Homebrew (macOS) - Recommended**

```bash
brew install uv
```

**Option 2: pip**

```bash
pip install uv
```

**Option 3: Windows (via pip or Scoop)**

```bash
# Using pip
pip install uv

# Or using Scoop (Windows)
scoop install uv
```

**Option 4: Manual installation**

Visit <https://docs.astral.sh/uv/getting-started/installation/> for platform-specific installers.

**Option 5: Pre-built binaries**

Download from <https://github.com/astral-sh/uv/releases/>

Verify installation:

```bash
uv --version    # Should show: uv 0.13.0 or higher
```

### Step 3: Install npm Dependencies

Install Node.js dependencies (including markdownlint):

```bash
npm install
```

### Step 4: Install Python Dependencies

Install all Python dependencies and MkDocs using uv:

```bash
npm run install-deps
```

This will:

1. Install Python 3.11 (if needed)
2. Create a Python virtual environment
3. Install MkDocs 1.6.1+
4. Install Material for MkDocs 9.7.6+
5. Install mkdocs-awesome-pages-plugin

### Step 5: Verify Installation

Verify all dependencies are installed correctly:

```bash
uv --version         # Should show: uv 0.13.0 or higher
node --version       # Should show: v24.x
npm --version        # Should show: 10.x or higher
npm run build        # Should complete without errors
```

---

## Running Development Server

Start the development server with live reload:

```bash
npm run dev
```

Output will show:

```
INFO     -  Building documentation...
INFO     -  Documentation built successfully
[10:30:00] Serving on http://127.0.0.1:8000/
[10:30:00] Watch mode enabled. Press 'Ctrl+C' to quit.
```

Open your browser to: **<http://localhost:8000>**

### Features

- ✅ Live reload on file changes
- ✅ Full-text search across all pages
- ✅ Dark/light mode toggle
- ✅ Mobile-responsive design
- ✅ Instant page loading

---

## Available Commands

All commands run from the `docs/` directory:

### Setup & Installation

```bash
npm install         # Install npm dependencies (markdownlint, etc.)
npm run install-deps # Install Python + MkDocs (requires uv to be installed first)
```

**To install uv:** See [Installation Step 2](#step-2-install-uv-python-package-manager)

### Development

```bash
npm run dev         # Start development server with live reload
                    # Access at: http://localhost:8000
npm run build       # Build static site for production
                    # Output: docs/site/
npm run lint        # Check markdown files for issues
npm run lint:fix    # Auto-fix markdown formatting issues
```

### Examples

```bash
# First time setup (macOS/Linux)
brew install uv
cd docs
npm install
npm run install-deps
npm run dev

# Subsequent development sessions
npm run dev

# Before committing documentation changes
npm run lint:fix

# Build for deployment
npm run build
```

---

## Project Structure

```
docs/
├── wiki/                           # Documentation content (24 files)
│   ├── .pages                      # Navigation structure
│   ├── index.md                    # Homepage
│   │
│   ├── getting-started/            # Getting started guides (3 files)
│   │   ├── index.md
│   │   ├── setup.md               # Development environment setup
│   │   └── running.md             # Running the application
│   │
│   ├── user-guide/                 # User documentation (5 files)
│   │   ├── index.md
│   │   ├── api-usage.md           # How to use the REST API
│   │   ├── database.md            # MongoDB operations
│   │   ├── authentication.md      # Authentication & authorization
│   │   ├── configuration.md       # Configuration options
│   │   └── troubleshooting.md     # Common issues & solutions
│   │
│   ├── api/                        # API documentation (4 files)
│   │   ├── index.md
│   │   ├── reference.md           # Complete API reference
│   │   ├── authentication.md      # API authentication
│   │   └── error-handling.md      # Error responses
│   │
│   ├── architecture/               # System design (2 files)
│   │   ├── index.md               # Architecture overview
│   │   └── configuration.md       # Configuration architecture
│   │
│   ├── configuration/              # Configuration guide (1 file)
│   │   └── index.md
│   │
│   └── contributing/               # Contributing guide (5 files)
│       ├── index.md
│       ├── guide.md               # How to contribute
│       ├── pull-requests.md       # PR process
│       ├── standards.md           # Code standards
│       └── quality/
│           └── index.md           # Testing guide
│
├── README.md                       # This file
├── package.json                    # Node.js configuration
├── pyproject.toml                  # Python dependencies
│
└── site/                           # Built documentation (generated)
    └── (static HTML files)
```

### Key Files

- **`mkdocs.yml`** (docs/) - Main MkDocs configuration
- **`pyproject.toml`** (docs/) - Python dependencies for MkDocs
- **`package.json`** (docs/) - Node.js scripts and dependencies
- **`wiki/.pages`** (docs/wiki/) - Navigation structure configuration

---

## Configuration

### MkDocs Configuration (`docs/mkdocs.yml`)

Located in the `docs/` directory, specifies:

- Site name and URL: `https://defra.github.io/trade-imports-fish-be`
- Theme: Material for MkDocs
- Plugins: Search, awesome-pages
- Markdown extensions: Code highlighting, tables, admonitions
- Documentation directory: `wiki/`

### Python Dependencies (`docs/pyproject.toml`)

Specifies Python packages needed:

```toml
[project]
name = "trade-imports-fish-be-docs"
requires-python = ">=3.11"
dependencies = [
    "mkdocs>=1.6.1",
    "mkdocs-material>=9.7.6",
    "mkdocs-awesome-pages-plugin>=2.10.1",
]
```

### Node.js Configuration (`docs/package.json`)

Specifies:

- Node.js engine: 24.x
- npm scripts for development
- Dependencies: uv, markdownlint-cli

---

## Development Workflow

### 1. Editing Documentation

Edit Markdown files in `docs/wiki/`:

```bash
# Example: Edit a page
vim docs/wiki/user-guide/api-usage.md
```

Changes appear instantly in the development server (live reload).

### 2. Adding New Pages

1. Create a new `.md` file in the appropriate directory
2. Add a `# Page Title` as the first line
3. Navigation auto-generates from directory structure
4. For custom navigation, edit `docs/wiki/.pages`

Example:

```bash
# Create new page
cat > docs/wiki/user-guide/new-guide.md << 'EOF'
# New Guide Title

Your content here.
EOF
```

### 3. Linting Before Commit

Always lint and fix markdown before committing:

```bash
npm run lint        # Check for issues
npm run lint:fix    # Auto-fix issues
```

### 4. Common Markdown Features

**Code blocks with syntax highlighting:**

````markdown
```bash
npm run dev
```

```java
public class Example {
    public static void main(String[] args) {}
}
```
````

**Tables:**

```markdown
| Column 1 | Column 2 |
|----------|----------|
| Data 1   | Data 2   |
```

**Admonitions (notes, warnings, etc.):**

```markdown
!!! note "Note Title"
    Note content goes here.

!!! warning "Warning!"
    Warning content.

!!! success "Success!"
    Success message.
```

**Tabs:**

```markdown
=== "Tab 1"
    Content for tab 1

=== "Tab 2"
    Content for tab 2
```

---

## Building & Deployment

### Building for Production

Build a static site ready for deployment:

```bash
npm run build
```

Output is in `docs/site/` - ready for deployment.

### Automatic GitHub Pages Deployment

Documentation automatically deploys to GitHub Pages when you push to the main branch:

```bash
git push origin main
```

The GitHub Actions workflow (`.github/workflows/docs-deploy.yml`) will:

1. Checkout code
2. Setup Node.js 24
3. Install Python 3.11
4. Install documentation dependencies
5. Build static site
6. Upload to GitHub Pages
7. Deploy

**Published URL:** <https://defra.github.io/trade-imports-fish-be>

### Manual Deployment

To deploy manually:

1. Build the site: `npm run build`
2. Copy `docs/site/` contents to your hosting
3. Or push to GitHub main branch (automatic)

---

## Troubleshooting

### Error: `sh: uv: command not found`

**Cause:** `uv` is not installed on your system.

**Solution:** Install uv using one of these methods (see [Installation Step 2](#step-2-install-uv-python-package-manager)):

**On macOS (Recommended):**

```bash
brew install uv
```

**On Linux/Windows with pip:**

```bash
pip install uv
```

**Then verify:**

```bash
uv --version    # Should show: uv 0.13.0 or higher
```

**After installing uv, run:**

```bash
cd docs
npm install
npm run install-deps
```

### Error: `Python 3.11 not found`

**Cause:** Python 3.11+ is not installed.

**Solution:**

1. Download Python 3.11+ from <https://www.python.org/>
2. Install it on your system
3. Verify: `python3 --version`
4. Then: `npm run install-deps`

### Error: Port 8000 already in use

**Cause:** Another process is using port 8000.

**Solution:**

Use a different port:

```bash
uv run mkdocs serve --dev-addr 127.0.0.1:8001
```

Then visit: <http://localhost:8001>

### Slow Installation

**Cause:** First-time installation installs Python and many packages.

**Solution:** This is normal. Installation takes 2-5 minutes on first run. Subsequent runs are much faster.

### Dependencies Won't Install

**Solution:**

Clear and reinstall:

```bash
cd docs
rm -rf .venv* node_modules/ uv.lock
npm install
npm run install-deps
```

### Build Fails

**Troubleshooting steps:**

1. Check Markdown syntax: `npm run lint:fix`
2. Verify all links are correct
3. Ensure all referenced images exist
4. Check for uv installation: `uv --version`
5. Clear cache: `rm -rf .venv* && npm run install-deps`

### Live Reload Not Working

**Solution:**

1. Clear browser cache
2. Restart dev server: `npm run dev`
3. Check browser console for errors

### npm run install-deps Hangs

**Cause:** uv is downloading Python or dependencies.

**Solution:** Wait 2-5 minutes for first-time installation. If it hangs longer:

```bash
# Cancel with Ctrl+C, then try:
uv --version    # Verify uv works
rm -rf .venv*   # Clear Python cache
npm run install-deps  # Try again
```

### Still Having Issues?

1. Verify all prerequisites are installed:

   ```bash
   node --version      # Should be v24.x
   npm --version       # Should be 10.x+
   python3 --version   # Should be 3.11+
   uv --version        # Should be 0.13.0+
   ```

2. Create an issue on GitHub with:
   - Your error message
   - Output of all version commands above
   - Your operating system and shell

---

## Features & Technology Stack

### Features

✨ **Theme & UI:**

- Material for MkDocs (professional, responsive design)
- Dark/light mode toggle
- Mobile-friendly interface
- Beautiful typography

✨ **Functionality:**

- Full-text search across all pages
- Code syntax highlighting (Java, Bash, Python, etc.)
- Tabbed content sections
- Admonitions (notes, warnings, tips, errors)
- Tables and definition lists
- Responsive design

✨ **Development:**

- Live reload development server
- Markdown linting with auto-fix
- Instant feedback on changes
- File watching

✨ **Automation:**

- GitHub Actions workflow
- Automatic GitHub Pages deployment
- Triggered on push to main branch

### Technology Stack

| Component | Version | Purpose |
| ----------- | --------- | --------- |
| **MkDocs** | 1.6.1+ | Static site generator |
| **Material for MkDocs** | 9.7.6+ | Professional theme |
| **Python** | 3.11+ | MkDocs runtime |
| **uv** | 0.13.0+ | Fast Python package manager (system installation) |
| **Node.js** | 24.x | Task runner and linting |
| **markdownlint** | 0.49.1+ | Markdown validation |

### Markdown Extensions

- `pymdownx.highlight` - Syntax highlighting
- `pymdownx.superfences` - Code blocks
- `pymdownx.tabbed` - Tabbed content
- `admonition` - Admonitions (notes, warnings)
- `tables` - Table support
- `attr_list` - Attributes on elements
- `def_list` - Definition lists

---

## Next Steps

### For First-Time Users

1. ✅ Install uv: `brew install uv` (or see [Installation Step 2](#step-2-install-uv-python-package-manager))
2. ✅ Run setup: `cd docs && npm install && npm run install-deps`
3. ✅ Start dev server: `npm run dev`
4. ✅ Open browser: <http://localhost:8000>
5. ✅ Review documentation structure
6. ✅ Read [development workflow](#development-workflow)

### For Contributors

1. Install uv (see step 1 above)
2. Create feature branch: `git checkout -b docs/my-feature`
3. Edit files in `docs/wiki/`
4. Lint before commit: `npm run lint:fix`
5. Test locally: `npm run dev`
6. Commit and push
7. Create Pull Request

### For Maintenance

1. Keep documentation in sync with code changes
2. Run linting regularly: `npm run lint:fix`
3. Review before releases: `npm run build`
4. Monitor deployment: Check GitHub Actions
5. Gather feedback from team

### Resources

- **MkDocs Documentation:** <https://www.mkdocs.org/>
- **Material Theme:** <https://squidfunk.github.io/mkdocs-material/>
- **Markdown Guide:** <https://www.markdownguide.org/>
- **uv Documentation:** <https://docs.astral.sh/uv/>
- **GitHub Actions:** <https://docs.github.com/actions>

---

## Summary

| Task | Command |
| ------ | --------- |
| Install uv | `brew install uv` (or [see alternatives](#step-2-install-uv-python-package-manager)) |
| First-time setup | `npm install && npm run install-deps` |
| Start development | `npm run dev` |
| Check for issues | `npm run lint` |
| Fix issues | `npm run lint:fix` |
| Build site | `npm run build` |
| Deploy | `git push origin main` |

---
