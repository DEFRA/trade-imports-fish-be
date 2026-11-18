---
name: create-update-mkdocs
description: Create or update technical documentation in docs/wiki/ for the Trade Imports Fish Backend. Document features, services, workflows, configuration, and architecture. Compare documentation with implementation; code is the source of truth.
---

# Technical Documentation for Trade Imports Fish Backend

Create or update focused technical Markdown documents under `docs/wiki/` organized by section: `getting-started/`, `user-guide/`, `api/`, `architecture/`, `configuration/`, or `contributing/`. One topic per page. Keep pages to ~60 lines and focused on a single subject.

`docs/wiki/` is the input folder for MkDocs (configured in `docs/mkdocs.yml`). All documents must be valid Markdown compatible with MkDocs Material theme.

**The implementation is the source of truth for current behavior.** When documentation disagrees with working code, update the documentation to describe the code as it exists now. Do not change code as part of documentation tasks.

## Project Structure

```
docs/
├── AGENTS.md                  # This file; project-specific agent guidance
├── README.md                  # Documentation setup and development guide
├── mkdocs.yml                 # MkDocs configuration
├── wiki/
│   ├── .pages                 # Navigation structure (edit for ordering)
│   ├── index.md               # Landing page (links to all sections)
│   ├── getting-started/
│   │   ├── index.md           # Getting started overview
│   │   ├── setup.md           # Development environment setup
│   │   └── running.md         # Running the application
│   ├── user-guide/
│   │   ├── index.md           # User guide overview
│   │   ├── api-usage.md       # Using the REST API
│   │   ├── database.md        # MongoDB operations
│   │   ├── authentication.md  # Authentication and authorization
│   │   ├── configuration.md   # Configuration properties
│   │   └── troubleshooting.md # Common issues and solutions
│   ├── api/
│   │   ├── index.md           # API overview
│   │   ├── reference.md       # Complete endpoint reference
│   │   ├── authentication.md  # API authentication
│   │   └── error-handling.md  # Error response formats
│   ├── architecture/
│   │   └── index.md           # System architecture and design
│   ├── configuration/
│   │   └── index.md           # Configuration management
│   └── contributing/
│       ├── index.md           # Contributing overview
│       ├── guide.md           # Contribution guidelines
│       ├── pull-requests.md   # PR process
│       ├── standards.md       # Code standards
│       └── quality/
│           └── index.md       # Testing and quality assurance
└── site/                      # Generated (git-ignored); do NOT edit
```

## Workflow

1. **Identify the subject and destination.** Translate the request into a specific feature, workflow, or component. Search `docs/wiki/` for the closest existing document. Reuse and update that file when it covers the subject; otherwise choose a concise filename under the appropriate section folder (not root). If spanning multiple concerns, create separate files in appropriate sections instead of one large document. Example: document a new config property in `configuration/index.md`, not in `api/reference.md`.

2. **Trace the behavior.** Locate the code that implements the subject. For controllers: check `src/main/java/.../controller/`. For services: check `src/main/java/.../service/`. For configuration: check `src/main/resources/application.yml` and configuration classes in `src/main/java/.../configuration/`. For entity definitions: check `src/main/java/.../domain/`. Read tests in `src/test/java/` as corroborating evidence for inputs, outputs, edge cases. Keep exploration focused on what the documentation needs to claim.

3. **Compare documentation with evidence.** For each existing claim, check it against the current implementation: code paths, configuration property names and defaults, endpoint paths, request/response structures, exception types. Treat implementation as authoritative; use tests to confirm behavior. Mark uncertainties rather than assuming. Verify port (8085), database name (trade-imports-fish-be), Java version (25), Spring Boot version (3.5).

4. **Create or revise documents.** `docs/wiki/index.md` is the landing page and must link to all major sections. If updating an existing page, preserve accurate content and update stale claims. If creating a new page, add it to the appropriate section folder and update `docs/wiki/.pages` for navigation ordering. When renaming or deleting pages, verify `.pages` and `index.md` links are updated. Keep scope focused; avoid duplicating content—link to other pages instead. Only document behavior directly supported by code, configuration, or tests; do not speculate.

5. **Verify the result.** 
   - Confirm file is under `docs/wiki/` and uses plain Markdown (no front matter required; first `#` heading is the page title).
   - Check relative links resolve: use `[Title](../section/page.md)` format for wiki links; use full GitHub URLs for source code outside `docs/`.
   - Verify documented commands exist: test `npm run dev`, `npm run build`, `mvn test`, etc.
   - Verify configuration names/defaults match `src/main/resources/application.yml` (e.g., `PORT: 8085`, `MONGO_DATABASE: trade-imports-fish-be`).
   - Verify endpoint paths match controllers (e.g., `/example` from `ExampleController`).
   - Run formatting: `cd docs && npm run lint:fix`
   - Run build validation: `cd docs && npm run build` (should complete without errors).
   - Report any behavior that could not be verified.
   - Confirm no code changes were made.

## MkDocs Site Conventions (Trade Imports Fish Backend)

- Pages under `docs/wiki/` are plain Markdown; no YAML front matter required. The first `#` heading is the page title. Example: `# API Reference` becomes the page title.
- `mkdocs.yml` is in `docs/` (not repository root) and specifies `docs_dir: wiki` and `site_dir: site`.
- Navigation is controlled by `docs/wiki/.pages` using the [awesome-pages plugin](https://pypi.org/project/mkdocs-awesome-pages-plugin/). Edit `.pages` to change section order/names.
- `docs/wiki/index.md` is the home page. Keep it as an overview with links to all major sections.
- Link between wiki pages using relative Markdown paths: `[Title](../section/page.md)` or `[Title](../section/page.md#anchor)`. MkDocs rewrites these to URLs; `npm run build` fails if links don't resolve.
- Link to source code, configuration files, or tests outside `docs/wiki/` using full GitHub URLs on the main branch: `https://github.com/DEFRA/trade-imports-fish-be/blob/main/src/main/java/.../ClassName.java`
- Use standard Markdown and fenced code blocks with language identifiers (bash, java, json, yaml, etc.). No escaping needed for template syntax.
- Use Material theme admonitions only if `mkdocs.yml` enables them (`pymdownx.superfences`, etc.); otherwise use **bold labels** or blockquotes for emphasis.

## Evidence and Writing Rules

- **Base claims on code paths.** Identify the narrowest code path that controls behavior. For REST endpoints, cite the controller method and annotations. For configuration, cite the property name and default. For services, cite the method and business logic.
- **Link correctly.** For documentation pages: relative links like `[Auth](authentication.md)`. For source: full GitHub URLs. For configuration: GitHub URLs to `application.yml`.
- **Avoid duplication.** Each fact belongs in one document. Link to that document from related pages instead of repeating content.
- **Match current code exactly.** Check environment variable names, default values, endpoint paths, field names, exception types, Java class names literally against the repository. Example: port is `8085` (not 8080), database is `trade-imports-fish-be` (not `trade-imports-fish`), application class is `uk.gov.defra.trade.imports.fish.be.Application`.
- **Never include real credentials.** No passwords, API keys, tokens, or secrets in examples. Use `<token>`, `localhost`, example values.
- **Separate observation from assumption.** State what the code does; qualify external behavior or intended design separately.
- **Keep scope focused.** Document only the subject requested. Update directly related links/references only.

## Done When

- A suitable page exists under `docs/wiki/` in the appropriate section and covers the requested subject.
- Every page is plain Markdown with a single `#` heading and no front matter; `npm run build` from `docs/` builds without errors.
- `docs/wiki/index.md` and `docs/wiki/.pages` are updated to reflect all pages.
- No duplicate content exists across pages; related facts are linked, not repeated.
- Each concrete claim is supported by implementation, configuration, or tests; uncertainties are qualified.
- Existing inaccurate claims are corrected to match the current code.
- Configuration names, defaults, endpoint paths, and examples match the codebase exactly.
- Links (wiki and GitHub) are verified to resolve.
- Markdown is formatted: `npm run lint:fix` passes.
- No code changes were made; documentation only reflects current state.
