# Changelog

All notable changes to SignPort are documented here.

This project uses SemVer-style mod versions. Release entries should include the Minecraft target when useful, for example `1.0.1+mc1.21.10`.

## Unreleased

- Added the optional bundled client foundation: anchor/permission sync, the portal-sign HUD lookup hint, and `config/signport-client.json` with a Cloth Config/ModMenu soft-dependency screen. Permission sync refreshes on join, respawn, and dimension change.
- Fixed source hygiene check failing on Windows due to CRLF line endings in workflow files.
- Logged a one-line anchor summary at server start (`Loaded N anchor(s): X in overworld, Y in the_nether`); legacy file migration now also runs eagerly at startup instead of on the first write.
- Auto-sync Modrinth project description from `docs/modrinth-listing.md` on every release; added hygiene gates to ensure both listing docs stay structurally complete.
- Added tab-completion of anchor names (in the player's current dimension) for `/sp tp <name>` and `/sp anchor delete <name>`.
- Paginated `/sp anchor list` with a clickable `[« Prev] page X/Y [Next »]` footer; out-of-range pages clamp to the last valid page.
- Added an optional case-insensitive substring filter to `/sp anchor list [filter] [page]`; the page footer preserves the active filter.
- Added `anchorListPageSize` config option (default 10) controlling how many anchors are shown per `/sp anchor list` page.
- Added optional anchor groups via `/sp anchor create <name> <group>` and `/sp anchor setgroup <name> <group>`, with grouped `/sp anchor list` headers and group-name tab-completion.
- Added `/sp anchor near [radius]` for distance-sorted anchors in the player's current dimension, using new `defaultNearRadius` config (default 128).
- Added `/sp anchor list --sort=name|distance|recent`; distance sorting shows meters from the player and recent sorting uses new anchor creation timestamps while keeping legacy anchors last.
- Added optional BlueMap integration that publishes anchors as map markers when BlueMap is installed, controlled by new `bluemapEnabled` config.
- Documented the updated phase workflow and added a source hygiene gate for stale `ResourceKey.location()` usage under MC 26.1.2 Mojang mappings.

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
