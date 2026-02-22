# CLAUDE.md

This file provides guidance for AI assistants (Claude and others) working on the **Ccweb** repository.

---

## Project Overview

**Ccweb** is a web project currently in its initial stage. The repository was created by `ccd930318` (Tony) and has a single initial commit that adds a placeholder `README.md`.

At the time of this writing, the project consists of:

```
Ccweb/
├── README.md       # Project name placeholder
└── CLAUDE.md       # This file
```

There is no application code yet. When code is added, update this file to reflect the actual structure, stack, and conventions.

---

## Repository Setup

| Item | Value |
|------|-------|
| Repo name | `Ccweb` |
| Default branch | `master` |
| Remote | `origin` (proxied via `127.0.0.1:61671`) |
| Git author | Claude (`noreply@anthropic.com`) for automated commits |
| Commit signing | SSH (`/home/claude/.ssh/commit_signing_key.pub`) |

### Branching convention

- Feature/task branches follow the pattern `claude/<short-description>-<session-id>` when created by Claude Code (e.g., `claude/add-claude-documentation-D3DhE`).
- Human-authored work lands on `master` (or `main` on the remote).
- Always push to the designated branch and never force-push to `master`/`main`.

### Git push

Always use the `-u` flag when pushing a branch for the first time:

```bash
git push -u origin <branch-name>
```

If the push fails with a network error, retry up to 4 times with exponential backoff (2 s, 4 s, 8 s, 16 s).

---

## Development Workflows

Because no technology stack has been chosen yet, the workflows below are placeholders. **Update this section as soon as a framework and toolchain are selected.**

### Expected workflow (template)

```bash
# 1. Install dependencies (update command to match the chosen package manager)
npm install          # Node.js / npm
# or
pip install -r requirements.txt   # Python

# 2. Start the development server
npm run dev

# 3. Run tests
npm test

# 4. Build for production
npm run build

# 5. Lint / format
npm run lint
npm run format
```

### When adding new tooling

After a package manager, linter, formatter, or test runner is introduced:
1. Update this file's **Workflows** section with the actual commands.
2. Add a `scripts` section to `package.json` (or equivalent) for all common tasks.
3. Document any required environment variables in a `.env.example` file (never commit real secrets).

---

## Code Conventions (to be filled in)

No source code exists yet. Once the stack is decided, document the following here:

- **Language & runtime** – e.g., TypeScript 5.x / Node 22, Python 3.12, Go 1.23, etc.
- **Framework** – e.g., Next.js, FastAPI, Gin
- **Formatting** – e.g., Prettier, Black, gofmt — with config file location
- **Linting** – e.g., ESLint (`eslint.config.js`), Ruff, golangci-lint
- **Testing** – e.g., Vitest, pytest, Go test
- **Import ordering** – internal vs external, alphabetical, etc.
- **File naming** – kebab-case, PascalCase components, snake_case Python, etc.
- **Directory structure** – where views, services, models, tests live

---

## AI Assistant Guidelines

### General principles

1. **Read before editing.** Always read a file before modifying it. Never guess at its contents.
2. **Minimal changes.** Only change what is needed to fulfil the current task. Avoid unsolicited refactoring, adding comments, or style changes.
3. **No secrets.** Never commit API keys, passwords, or tokens. Use environment variables and `.env.example` for documentation.
4. **Security first.** Do not introduce OWASP Top 10 vulnerabilities (SQLi, XSS, SSRF, command injection, etc.).
5. **No over-engineering.** Avoid premature abstractions, unused helpers, or backward-compatibility shims for code that does not need them.

### Task planning

Use the `TodoWrite` tool to track multi-step tasks. Mark each todo as `in_progress` before starting it and `completed` immediately after finishing it.

### Commits

- Write concise, imperative commit messages (e.g., `Add user authentication endpoint`).
- Focus commit messages on *why*, not *what* (the diff shows the what).
- Each commit should represent a single logical change.
- Never amend a previous commit unless explicitly requested; always create a new commit.

### File creation

Prefer editing existing files over creating new ones. Only create files when genuinely required by the task.

---

## Notes for the First Developer Sprint

When the first real code lands, come back and update:

- [ ] **Stack** – language, runtime version, framework
- [ ] **Install** – exact command to bootstrap the project
- [ ] **Run** – how to start the dev server
- [ ] **Test** – how to run the test suite and what passes/fails mean
- [ ] **Build** – production build command and output directory
- [ ] **Lint/Format** – commands and config file paths
- [ ] **Environment variables** – required vars and where to get values
- [ ] **Directory structure** – annotated tree of the source layout
- [ ] **Architecture decisions** – any notable ADRs or design choices
