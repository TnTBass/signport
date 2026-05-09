# Changelog

All notable changes to SignPort are documented here.

This project uses SemVer-style mod versions. Release entries should include the Minecraft target when useful, for example `1.0.1+mc1.21.10`.

## Unreleased

## 1.1.1+mc1.21.10 - 2026-05-08

- Added CurseForge publication to the tag-triggered release workflow using the existing release metadata.
- Added Modrinth publication to the tag-triggered release workflow and a source hygiene gate to keep it wired in.
- Added mechanical source hygiene gates for unit-test companions, wrapper checksum hardening, and obvious duplicate portal validation.
- Added checksum verification for the Gradle wrapper distribution.
- Reused resolved portal-sign destinations during interaction handling and added indexed anchor lookup helpers.
- Protected portal-marker signs from unauthorized break attempts even when their target anchor is currently invalid or missing.
- Hardened GitHub Actions permissions and release tag validation.
- Added a tag-triggered GitHub Release workflow that builds with Java 25, uses changelog notes, and attaches the mod and sources jars.
- Added `config/signport.json` for permission fallback defaults, protected-action op level, cross-dimension portal signs, and safe teleport search.
- Made sign and `/signport tp` teleports search for a safe centered destination near the anchor and fail gracefully when none is available.
- Added an in-game validation checklist for SignPort permission and portal behavior.
- Updated source links for the public SignPort repository and Modrinth listing prep.

## 1.1.0+mc1.21.10 - 2026-05-08

- Added LuckPerms-compatible permission checks through `fabric-permissions-api`.
- Added permission nodes for anchor commands, portal sign creation/editing/breaking, portal sign use, and `/signport tp`.

## 1.0.1+mc1.21.10 - 2026-05-08

- Updated mod metadata and project attribution.
- Documented commands, portal sign format, dimension support, and front/back sign behavior.
- Made invalid or blank dimension IDs fail gracefully instead of crashing portal sign resolution.
- Made portal interaction check the player-facing side first, then the opposite side.
- Aligned the build, CI, and mod metadata around Java 25.
- Added Gradle daemon JVM criteria, toolchain resolution, Dependabot, editor defaults, and JUnit test coverage for portal sign parsing.
- Added a changelog capture gate for releasable changes.
- Added a release reminder gate when unreleased changelog entries exist.
- Skipped the changelog capture gate for Dependabot actors and update branches.
- Updated Gradle, GitHub Actions, and JUnit dependencies through Dependabot.
- Allowed versioned release sections to satisfy the changelog capture gate during release prep.
