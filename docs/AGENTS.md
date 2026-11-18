# Documentation Agent Guidelines

## Scope and Tooling

- These instructions apply only to `docs/` and its descendants.
- Uses [MkDocs][mkdocs] with the [Material theme][material].
- Navigation uses [mkdocs-awesome-pages-plugin][mkdocs-pages]
  and `.pages` files. Site settings are in [mkdocs.yml](mkdocs.yml).
- See [README.md](README.md) for documentation setup and maintenance details.
- For general documentation authoring standards, refer to the `create-update-backend-docs` skill.

## Directory Layout

- `wiki/index.md` is the current landing page; keep links to all pages here
  so readers and agents can find documentation through targeted lookup.
- `wiki/<section>/` holds `getting-started`, `user-guide`, `api`,
  `architecture`, `configuration`, and `contributing` documentation.
- `.pages` files control navigation order and grouping in their folders.
- `site/` is generated, git-ignored build output; never edit it manually.
- This file is project-specific agent guidance, not a wiki page; exclude it from navigation and the wiki index.

## Commands

Run these commands from `docs/`:

- `npm install` - install the declared Node.js development dependencies.
- `npm run install-deps` - install Python dependencies via uv.
- `npm run lint` - validate Markdown.
- `npm run lint:fix` - auto-fix Markdown issues; review the resulting changes.
- `npm run build` - build the static site to `docs/site/`.
- `npm run dev` - serve with live reload at `localhost:8000`.

The package requires Node.js 24.x; Python dependencies require Python 3.11+
and uv. Run lint and build for wiki or navigation changes. Report missing
tooling or configuration explicitly rather than silently skipping validation.

## Documentation Structure

### Section Organization

- **getting-started/**: Setup, installation, and first-run guides
- **user-guide/**: How to use, configure, and troubleshoot
- **api/**: REST API reference, authentication, error handling
- **architecture/**: System design, components, patterns
- **configuration/**: Configuration properties and environment variables
- **contributing/**: Contribution guidelines, standards, testing

### Writing Standards

- Keep each wiki page focused on a single topic (max 60 lines recommended).
- Start every page with a single `#` heading (page title).
- Link to other documentation using relative Markdown links: `[Title](../section/page.md)`.
- Do not duplicate content across pages; link instead.
- Provide examples that match current code and configuration exactly.
- Use code blocks with proper syntax highlighting (bash, java, json, yaml).

### Page Metadata

Currently, **no YAML frontmatter is required** in wiki pages. MkDocs generates page titles from the first `#` heading. If frontmatter becomes required:

- Add `tags` list with section (e.g., `getting-started`) and topic (e.g., `commands`)
- Keep tags minimal and semantic

### Procedures and How-Tos

New step-by-step procedures should be:

- Placed in appropriate section subdirectories (`getting-started/`, `user-guide/`, `contributing/`)
- Titled with action verbs: "Running the Application", "Setting Up MongoDB"
- Broken into clear steps with code examples
- Linked from section index pages

## Content Guidelines

### Accuracy

- Base all behavioral descriptions on current codebase
- Link to source code with full GitHub repository URLs (outside `docs/wiki/`)
- Include actual configuration property names and defaults
- Test commands before documenting them
- Note any assumptions or external dependencies

### Code Examples

- Match the current Java version (25), Spring Boot version (3.5), and Maven version
- Use actual endpoint paths from controllers (e.g., `/example` not `/api/imports`)
- Use correct default port (8085, from `application.yml`)
- Never include real credentials or sensitive data
- Always use `localhost` for local examples

### Links

- **To wiki pages**: Use relative paths like `[Title](../api/reference.md)`
- **To source code**: Use full GitHub URLs: `https://github.com/DEFRA/trade-imports-fish-be/blob/main/src/...`
- **To configuration files**: Use GitHub URLs outside `docs/`
- Verify all links resolve before committing

## Tag Vocabulary

Use tags to categorize documentation (if frontmatter is added):

**Sections** (required):

- `getting-started`, `user-guide`, `api`, `architecture`, `configuration`, `contributing`

**Topics** (optional, use as needed):

- `commands` - CLI or npm commands
- `validation` - Input validation and testing
- `testing` - Test setup and execution
- `credentials` - Authentication and secrets
- `troubleshooting` - Debugging and common issues
- `deployment` - Production setup and deployment
- `environment-variables` - Configuration via env vars
- `mongodb` - Database-specific guidance

## CI/CD: Documentation Update Checker

The `.github/workflows/docs-update-checker.yml` workflow enforces documentation updates:

- **Trigger**: Pull requests (unless labeled `no-docs`)
- **Requirement**: Changes must include updates to files under `docs/wiki/`
- **Exception**: Add the `no-docs` label if:
  - Changes are internal refactoring only
  - Changes don't affect user-facing functionality
  - Documentation is not needed for this PR
- **Scope**: Changes to `docs/AGENTS.md` or `docs/README.md` alone do NOT satisfy the check

## Implementation Notes

### For Agents Updating Docs

1. Identify which section(s) the change affects
2. Find or create the appropriate markdown file
3. Compare documentation with current code/configuration
4. Update inaccurate information
5. Add new information if not covered elsewhere
6. Run `npm run lint:fix` to format
7. Test locally: `npm run dev` to preview at `http://localhost:8000`
8. Run `npm run build` to validate the full build
9. Verify all links resolve and no pages are broken

### For Code Changes

When code changes affect documented features:

1. Update the relevant wiki page(s) in the same PR
2. Verify examples still work
3. Update port/URL references if infrastructure changes
4. Update configuration examples if defaults change
5. Update API reference if endpoints change

### Consistency Checks

Before committing documentation:

- [ ] All port references use 8085 (default from `application.yml`)
- [ ] All endpoints match actual controller paths (e.g., `/example`)
- [ ] All Java versions reference Java 25
- [ ] All MongoDB examples use correct database name: `trade-imports-fish-be`
- [ ] All examples use `localhost` (not real hosts)
- [ ] No real credentials or tokens appear in examples
- [ ] Markdown lint passes: `npm run lint:fix`
- [ ] Build succeeds: `npm run build`

[mkdocs]: https://www.mkdocs.org/
[material]: https://squidfunk.github.io/mkdocs-material/
[mkdocs-pages]: https://pypi.org/project/mkdocs-awesome-pages-plugin/
