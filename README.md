CDP Java Spring Boot backend template.

- [Docker Compose](#docker-compose)
- [MongoDB](#mongodb)
- [Inspect MongoDB](#inspect-mongodb)
- [Testing](#testing)
- [Running](#running)
- [Publishing](#publishing-feature-branches)
- [SonarCloud](#sonarcloud)
- [Dependabot](#dependabot)
- [Documentation](#documentation)
- [Setup](#setup)
- [Git Pre-commit Hook](#git-pre-commit-hook)
- [Signed Commits](#signed-commits)

### Docker Compose

A Docker Compose template is in [compose.yml](compose.yml).

A local environment with:

- Floci for AWS services (S3, SQS)
- Redis
- MongoDB
- This service.
- A commented out frontend example.

```bash
docker compose --profile services up --build -d
```

A more extensive setup is available
in [github.com/DEFRA/cdp-local-environment](https://github.com/DEFRA/cdp-local-environment)

### MongoDB

#### MongoDB via Docker

Run infrastructure services (MongoDB and Floci):

```bash
docker compose up -d mongodb floci
```

#### MongoDB locally

Alternatively install MongoDB locally:

- Install [MongoDB](https://www.mongodb.com/docs/manual/tutorial/#installation) on your local
  machine
- Start MongoDB:

```bash
sudo mongod --dbpath ~/mongodb-cdp
```

#### MongoDB in CDP environments

In CDP environments a MongoDB instance is already set up
and the credentials exposed as enviromment variables.

### Inspect MongoDB

To inspect the Database and Collections locally:

```bash
mongosh
```

You can use the CDP Terminal to access the environments' MongoDB.

### Testing

Run the tests with:

```bash
mvn test
```

There are also application level tests run by running a full Spring Boot application backed
by [Testcontainers](https://testcontainers.com/).
These tests do not use mocking of any sort and read and write from the containerized database.

```bash
mvn clean verify
```

### Running

Run the application:

```bash
mvn spring-boot:run
```

### Publishing feature branches

The [Publish workflow](.github/workflows/publish.yml) publishes stable releases on pushes to `main`.
To publish a feature branch for testing, open **Publish** in GitHub Actions, select **Run workflow**,
choose the branch, and run it. Supported branch names follow
`feature/FISB-1234-description`, `bugfix/FISB-1234-description`, `ci/FISB-1234-description`, or
`chore/FISB-1234-description`, using a lowercase, hyphen-separated description.

The workflow tests the backend and publishes a unique prerelease image to ECR, not Docker Hub.
After it succeeds, select the resulting version in the CDP Portal. Stable-tag and version-bump
safeguards prevent a prerelease from reusing a stable image tag.

### SonarCloud

Example SonarCloud configuration is available in the [check-pull-request.](.github/workflows/check-pull-request.yml)

### Dependabot

We have added an example dependabot configuration file to the repository. You can enable it by
renaming
the [.github/example.dependabot.yml](.github/dependabot.yml) to `.github/dependabot.yml`

### About the licence

The Open Government Licence (OGL) was developed by the Controller of Her Majesty's Stationery
Office (HMSO) to enable
information providers in the public sector to license the use and re-use of their information under
a common open
licence.

It is designed to encourage use and re-use of information freely and flexibly, with only a few
conditions.

### Documentation

The service includes comprehensive technical documentation built with MkDocs.

#### Prerequisites

Before building the documentation, ensure you have the following installed:

**Python 3.8+**:

```bash
python3 --version
```

If not installed, see [python.org](https://www.python.org/downloads/) for installation instructions.

**MkDocs**:

```bash
mkdocs --version
```

If not installed, install via pip:

```bash
pip install mkdocs mkdocs-material
```

**Node.js & npm** (for Prettier):

```bash
node --version
npm --version
```

If not installed, see [nodejs.org](https://nodejs.org/en/download/) for installation instructions.

#### Building Documentation

Build the documentation site:

```bash
mkdocs build
```

This generates a static HTML site in the `_site/` directory.

#### Viewing Documentation Locally

Serve the documentation locally with live reload:

```bash
mkdocs serve
```

Then open http://localhost:8000 in your browser. The site will automatically reload when documentation files change.

#### Documentation Structure

Documentation is organized in the `docs/` folder with one file per component:

- `docs/index.md` — Navigation hub and overview
- `docs/architecture.md` — Project structure, technology stack, local dev setup
- `docs/api.md` — REST API endpoints and examples
- `docs/services.md` — Business logic and service layer
- `docs/configuration.md` — Configuration options and environment variables
- `docs/error-handling.md` — Exception handling and error responses
- `docs/filters.md` — Request tracing and middleware
- `docs/monitoring.md` — Logging, metrics, and observability

Documentation is automatically deployed to GitHub Pages via the [build-docs.yml](.github/workflows/build-docs.yml) workflow on every push to the main branch.

### Setup

Before making your first commit, follow the [Git Pre-commit Hook](#git-pre-commit-hook) and [Signed Commits](#signed-commits) setup.

### Git Pre-commit Hook

This repository includes a Git pre-commit hook that automatically updates documentation when you commit changes. The hook requires the Copilot CLI and GitHub authentication.

#### Prerequisites

1. **Copilot CLI** — check if it's already installed:

   ```bash
   copilot --version
   ```

   If not installed, install it:

   ```bash
   npm install -g @github/copilot
   ```

   Then verify installation:

   ```bash
   copilot --version
   ```

2. **Fine-grained Personal Access Token** — create one on GitHub:
   - Go to [GitHub Settings > Personal access tokens > Fine-grained tokens](https://github.com/settings/personal-access-tokens/new)
   - **Token name:** e.g., "Trade Imports Fish BE - Pre-commit Hook"
   - **Expiration:** Choose a reasonable duration (e.g., 90 days)
   - **Repository access:** Select "Public repositories"
   - **Permissions:**
     - Set **Copilot Chat** to `Read-only`
     - Set **Copilot request** to `Read-only`
   - Click **Generate token** and copy it immediately (you won't see it again)

#### Setup Steps

1. **Install the repository's local development dependencies** (requires Node.js and npm):

   ```bash
   npm ci
   ```

   This installs the pinned Prettier version used by the hook.

2. **Make the pre-commit hook executable:**

   ```bash
   chmod +x .githooks/pre-commit
   ```

3. **Configure Git to use the .githooks directory:**

   ```bash
   git config core.hooksPath .githooks
   ```

4. **Set the GitHub authentication token** — choose one of these environment variables:

   ```bash
   export COPILOT_GITHUB_TOKEN="ghp_xxxxxxxxxxxxxxxxxxxx"
   # OR
   export GH_TOKEN="ghp_xxxxxxxxxxxxxxxxxxxx"
   # OR
   export GITHUB_TOKEN="ghp_xxxxxxxxxxxxxxxxxxxx"
   ```

   To persist it across terminal sessions, add the export to your shell profile (`~/.bashrc`, `~/.zshrc`, etc.):

   ```bash
   echo 'export GITHUB_TOKEN="ghp_xxxxxxxxxxxxxxxxxxxx"' >> ~/.zshrc
   source ~/.zshrc
   ```

5. **Verify setup** — make a small change to a non-documentation file and run:

   ```bash
   git add your-changes
   git commit -m "test: verify pre-commit hook"
   ```

   The hook should run and automatically update `docs/` if needed.

#### Bypassing Pre-commit Hooks

If you need to skip the pre-commit hook for a specific commit (e.g., during troubleshooting or when the hook fails), use the `--no-verify` flag:

```bash
git commit --no-verify -m "your commit message"
```

> **⚠️ Use with caution:** Skipping the hook may result in stale documentation. Only use `--no-verify` when necessary, and ensure documentation is updated separately if needed.

**When to use `--no-verify`:**

- The hook is misconfigured or temporarily broken
- You're troubleshooting issues and need to commit changes quickly
- The documentation doesn't require updates for this specific commit
- You'll update documentation separately before merging to main

### Signed Commits

Commits to this repository must be signed.

> 🔑 **We prefer signing commits with SSH keys.** This guide covers SSH only. GitHub also supports GPG; see
> [GPG alternative](#gpg-alternative) if you prefer to use it instead.

Steps 1 to 3 are required. Step 4 is optional.

#### 1. Prerequisites

- Git 2.34 or later is required for SSH signing. Check your version:

  ```bash
  git --version
  ```

- You already have an SSH key pair. Key creation is outside this guide;
  see [GitHub's SSH key generation documentation](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent).
- Find the filename of your public key (it ends in `.pub`):

  ```bash
  ls ~/.ssh/*.pub
  ```

  The commands in this guide use `~/.ssh/id_ed25519.pub` as an example. **If your key has a different name** (for example
  `~/.ssh/github_signing.pub`), replace `id_ed25519` with your key's name in every command. Otherwise you will get a
  `No such file or directory` error.

- Use a Bash-compatible terminal, such as Terminal on macOS/Linux or Git Bash on Windows.

#### 2. Configure Git to sign with SSH

These settings apply globally to all your repositories. Repository-local settings can override them.

```bash
git config --global gpg.format ssh
git config --global user.signingkey ~/.ssh/id_ed25519.pub
git config --global commit.gpgsign true
```

- `gpg.format ssh` selects SSH instead of GPG for signing.
- `user.signingkey` points to your SSH public key. The matching private key must be available to your SSH agent;
  if needed, load it into your running agent with `ssh-add ~/.ssh/id_ed25519`.
- `commit.gpgsign true` signs new commits automatically. The setting keeps its name even when signing with SSH.

Optionally, sign tags automatically too:

```bash
git config --global tag.gpgsign true
```

Check the email Git will use from inside this repository:

```bash
git config --get user.email
```

It must match a verified email on your GitHub account under **Settings > Emails**; otherwise commits can show as **Unverified**.
If necessary, set it for this repository, replacing the example with your verified email:

```bash
git config user.email "your-verified-email@example.com"
```

#### 3. Add your SSH key to GitHub as a signing key

1. Display your public key and copy the entire output:

   ```bash
   cat ~/.ssh/id_ed25519.pub
   ```

2. Go to **GitHub > Settings > SSH and GPG keys > New SSH key**.
3. Give it a descriptive title, set **Key type** to **Signing Key**, paste the public key and select **Add SSH key**.
   Only ever share the `.pub` file; never paste your private key.

> ⚠️ **Important: authentication keys and signing keys are separate.**
>
> A key added as an **Authentication Key** (used for cloning, pushing and pulling) does **not** verify commits.
> If you use the same key for both, add it to GitHub twice: once with **Key type** set to **Authentication Key**
> and once with it set to **Signing Key**.

That's it: your commits will now be signed automatically.

#### Already working on a ticket branch?

Signing applies to new commits only; commits made before this setup stay unsigned. You don't need to switch branches.
To sign your latest commit as well, if it is your own and no one else is working from it, run:

```bash
git commit --amend --no-edit --only -S
```

This changes the commit ID. If you have already pushed that commit, coordinate with your team before updating the remote;
rewriting a shared branch can disrupt other people's work. For multiple earlier commits, agree with your team on how to
rewrite the branch history before proceeding.

#### 4. Optional: check your commits are signed

You don't need to do this. Use these checks if you want to confirm your setup or are troubleshooting.

**On GitHub:** push a commit, then open it on GitHub (in the branch's commit history or the pull request's **Commits**
tab) and confirm it shows **Verified**. Select the badge to see the signature details.

**Locally:** Git can't check SSH signatures on your machine until you set up an allowed signers file. Without it,
`git log --show-signature` reports a `gpg.ssh.allowedSignersFile` error. This only affects local checks; GitHub checks
signatures against the signing keys on your account. To set it up, run these once from inside this repository, so the
email matches your commits:

```bash
echo "$(git config --get user.email) $(cat ~/.ssh/id_ed25519.pub)" >> ~/.ssh/allowed_signers
git config --global gpg.ssh.allowedSignersFile ~/.ssh/allowed_signers
git log --show-signature -1
```

The last command should show `Good "git" signature for <your-email>` for a signed commit. See Git's
[`gpg.ssh.allowedSignersFile` documentation](https://git-scm.com/docs/git-config#Documentation/git-config.txt-gpgsshallowedSignersFile)
for the file format.

#### GPG alternative

GitHub also supports signing with GPG. If you prefer GPG, skip the SSH-specific parts of this guide: the SSH key and
Git 2.34 requirements in step 1, the SSH commands in steps 2 and 3, and the local check in step 4. Instead, follow
GitHub's guides to [tell Git about your GPG key](https://docs.github.com/en/authentication/managing-commit-signature-verification/telling-git-about-your-signing-key#telling-git-about-your-gpg-key)
and [add it to your GitHub account](https://docs.github.com/en/authentication/managing-commit-signature-verification/adding-a-gpg-key-to-your-github-account).
The email check in step 2 still applies, and you can still check for **Verified** on GitHub. See
[About commit signature verification](https://docs.github.com/en/authentication/managing-commit-signature-verification/about-commit-signature-verification)
for how the options compare.

#### Troubleshooting

- **Commit shows Unverified:** check that the key is registered as a **Signing Key**, not just an **Authentication Key**,
  and that the commit email matches a verified email on your GitHub account. Inspect the commit's email with
  `git log -1 --format='%ae %ce'` (author and committer).
- **Commit fails with a signing or key error:** check that the `user.signingkey` path exists
  (`git config --get user.signingkey`) and that your key is loaded in the SSH agent (`ssh-add -l`; load it with
  `ssh-add ~/.ssh/id_ed25519`).
- **`gpg.ssh.allowedSignersFile` error:** this only affects local checks such as `git log --show-signature`, not GitHub.
  See [step 4](#4-optional-check-your-commits-are-signed).
- **Older Git version:** [upgrade Git](https://git-scm.com/downloads) to 2.34 or later.
