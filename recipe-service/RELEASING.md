# Releasing the Recipe Service Artifact

This artifact (`com.miele.ckb:recipe-service`) is consumed by other
Miele CKB services via Maven. Follow these rules strictly.

## Versioning — Semantic Versioning

- **MAJOR** — breaking change to the public API or wire format
- **MINOR** — backwards-compatible additions
- **PATCH** — backwards-compatible bug fixes

Development versions carry the `-SNAPSHOT` suffix.

## Snapshots vs. Releases

| Kind     | Version example     | Repo                          | Mutable? |
|----------|---------------------|-------------------------------|----------|
| Snapshot | `0.2.0-SNAPSHOT`    | `libs-snapshot-local`         | yes      |
| Release  | `0.2.0`             | `libs-release-local`          | **NO**   |

A release coordinate (`groupId:artifactId:version`) is **immutable**:
once published it must never be overwritten.

## How to cut a release

1. Make sure `main` is green.
2. Tag the commit: `git tag v0.2.0 && git push origin v0.2.0`
3. GitLab CI runs the `deploy:release` job, which:
    - sets the project version to `0.2.0`,
    - runs `mvn deploy` to the releases repo in Artifactory.
4. Bump `pom.xml` on `main` to the next `-SNAPSHOT` (e.g. `0.3.0-SNAPSHOT`).

## Dependency hygiene

Before every release run:
