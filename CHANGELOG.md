# Changelog

All notable changes to SignPort are documented here.

This project uses SemVer-style mod versions. Release entries should include the Minecraft target when useful, for example `1.0.1+mc1.21.10`.

## Unreleased

- Corrected the Modrinth install metadata so SignPort is shown as required on servers and optional on clients.
- Added a client-side anchor creation form to the anchor browser for players with anchor creation permission.

## 2.1.1+mc26.1.2 - 2026-05-16

- Improved sign editor suggestions for servers with many anchors: longer result lists now scroll cleanly, clicking outside the visible list no longer selects hidden entries, and choosing an anchor can fill the destination dimension line for you.

## 2.1.0+mc26.1.2 - 2026-05-14

- Restored the optional client-side SignPort UX on MC 26.1.2: synced anchor data, portal-sign HUD hints, the anchor browser, sign-editor autocomplete, the SignPort Template form, and the ModMenu/Cloth Config settings entry.
- Added mouse-wheel scrolling to the anchor browser and clamped rendering/clicks to the visible list area.
- Fixed multiplayer client crashes caused by the MC 26.1.2 client entrypoint, keybind, and sign-editor mixin changes.
- Fixed redundant full anchor syncs when players change dimensions.
- Fixed raw coordinate teleports using the wrong dimension context.
- Fixed legacy overworld anchors not loading after upgrading pre-MC-26.1.2 worlds.
- Updated the mod icon, authorship, project links, optional dependency metadata, README, listing descriptions, and BlueMap integration docs.

## 2.0.2+mc26.1.2 - 2026-05-14

- Disabled optional client-side SignPort hooks in release jars to avoid multiplayer join crashes on clients.

## 2.0.1+mc26.1.2 - 2026-05-14

- Fixed a multiplayer client join crash by requesting the initial SignPort anchor sync only after the local client player exists.

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
