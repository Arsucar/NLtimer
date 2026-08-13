# Contributing to NLtimer

Thank you for considering contributing to NLtimer! This project uses a structured issue-based workflow to ensure quality and maintainability.

## 1. Before You Start

- **Read the docs first**: Check `docs/` folder for architecture, components, and conventions.
- **Search existing issues**: Make sure your issue/feature isn't already open.
- **Discuss first**: For big changes, start a Discussion thread or chat with maintainer.

## 2. Create an Issue

All changes **must** start with a GitHub Issue:
- Bug → Use `Bug Report` template
- Feature/Improvement → Use `Feature Request` template
- Other tasks → Use `Feature Request` with appropriate title (e.g. "feat: ...")

**Required fields**:
- Clear title with prefix (`bug: ` or `feat: `)
- Detailed description (use templates)
- For features: UI spec + Logic spec + Acceptance Criteria

## 3. PR Process

1. Fork the repo and create branch `feature/xxx` or `bugfix/yyy`
2. Implement changes
3. Submit PR **only after** Issue is closed/merged
4. In PR description:
   - Link to Issue: `Fixes #123`
   - Describe changes
   - Checklist completed

## 4. Code Style

- Follow Detekt rules (`./gradlew detekt`)
- Use Kotlin idioms, Compose best practices
- Add tests for new features
- Update docs if needed

## 5. CI / Release

- Changes to `feature/*` or `bugfix/*` trigger CI
- Release on `v*.*.*` tag

## 6. Agent / AI Workflow (for maintainers)

Use OpenCode workflow: Comment `/oc` or `/opencode` on PRs/Issues for automated help.

---

*This project follows RikkaRs-style conventions for consistency.*
