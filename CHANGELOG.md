# Changelog

All notable changes to SignPort are documented here.

This project uses SemVer-style mod versions. Release entries should include the Minecraft target when useful, for example `1.0.1+mc1.21.10`.

## Unreleased

- Clarified the public BlueMap integration description.
- Documented server and client config settings in the README and listing descriptions.

## 2.0.0+mc26.1.2 - 2026-05-14

- Added client-side SignPort helpers when both client and server have SignPort installed: synced anchor/permission data, HUD lookup hints for portal signs, the anchor browser, sign-editor autocomplete, and a permission-gated SignPort Template form.
- Added the SignPort anchor browser on the default `J` keybind, with dimension tabs, search, name/distance/recent sorting, collapsible group sections, and synced-cache teleport actions.
- Added portal sign editor autocomplete and the SignPort Template form to make valid portal signs easier to create from the synced anchor cache.
- Added tab-completion of anchor names for `/sp tp <name>` and `/sp anchor delete <name>`.
- Added paginated `/sp anchor list [filter] [page]`, clickable previous/next navigation, configurable `anchorListPageSize`, and case-insensitive filtering.
- Added optional anchor groups via `/sp anchor create <name> <group>` and `/sp anchor setgroup <name> <group>`, with grouped list output and group-name tab-completion.
- Added `/sp anchor near [radius]`, `defaultNearRadius`, and `/sp anchor list --sort=name|distance|recent`.
- Added optional BlueMap integration for publishing anchors as map markers, controlled by `bluemapEnabled`.
- Added startup anchor summary logging and eager legacy file migration.
- Added release/docs hygiene improvements, including Modrinth description sync, listing validation gates, workflow/source hygiene fixes, and stale `ResourceKey.location()` checks.
- Updated Modrinth publishing to use the `signport` project slug.

## 1.2.1+mc26.1.2 - 2026-05-10

- Fixed per-dimension SavedData files being created in DIM-1/data and DIM1/data even when no anchors existed there, causing warnings on MC 26.1 world format migration.
- Consolidated anchor storage into a single world-level file (`<world>/data/signport.dat`); anchors now carry a dimension key so names only need to be unique per dimension.
- Automatically migrates legacy DIM-1/data and DIM1/data signport files left behind by the MC 26.1 world format change, with best-effort cleanup of the now-empty directories.
- Fixed cross-dimension signs: an explicit dimension on line 3 now takes priority over a same-named anchor in the current dimension.
- Added `nether` and `end` as accepted shorthand dimension names on signs (in addition to `the_nether`, `the_end`, and `overworld`).

## 1.2.0+mc26.1.2 - 2026-05-10

- Ported to Minecraft 26.1.2 with official (Mojang) mappings.

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
