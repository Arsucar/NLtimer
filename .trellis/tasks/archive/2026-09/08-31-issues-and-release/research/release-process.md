# Research: release-process

- **Query**: Version numbers, GitHub release workflow, commits since last tag
- **Scope**: internal
- **Date**: 2026-08-31

## Findings

### Version source

| Location | Value |
|---|---|
| `gradle.properties` `APP_VERSION_NAME` | `0.3.7` |
| `gradle.properties` `APP_VERSION_CODE` | `307` |
| `app/build.gradle.kts` | reads those properties; APK name `NLtimer-v${APP_VERSION_NAME}-${variant}` |
| Latest git tag / GitHub Release | `v0.3.10` (published 2026-08-12) |
| `docs/agent/01-project-overview.md` | still `0.1.5 (build 105)` — stale |

No version-bump script found. Bump is manual in `gradle.properties` (+ docs).

Gradle properties lag the latest tag: 0.3.8–0.3.10 tags exist while properties stayed 0.3.7.

### Release mechanism

`.trae/skills/publish-release.md`:

- Changelog from `git log` since last release tag; bilingual; ≤20 bullets; no jargon
- Confirm changelog with user **before** publishing
- Publish by pushing `v*.*.*` tag; **do not** build APK locally
- Release title = version; body = changelog

Workflow file was not found via glob in this session (may be gitignored from agent view or named under `.github/workflows/`). Latest release asset `NLtimer-v0.3.10-release.apk` was uploaded by `github-actions[bot]`, so tag-triggered Actions exists on GitHub.

### Commits since `v0.3.10`

```
8851a298 fix: 多模块 bug 修复与代码清理发布
89a2af48 feat: 采用更好的 Issue/PR 工作流机制
```

Plus this task’s archive-zone work before next tag.

### Related task

`.trellis/tasks/08-16-review-and-release/` still `in_progress` (review of previously uncommitted fixes). Those fixes landed in `8851a298`. Not a blocker for this task.

### Next version

Recommended: **0.3.11 / 311** (follow last tag, not the stale gradle 0.3.7).
