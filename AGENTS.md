# Agent Handoff Document

This file is the canonical orientation document for any AI agent (Claude Code, Codex, etc.) working on SignPort. It is committed to git so a fresh clone has everything needed to be productive.

If you are starting a session, read this file completely before making changes.

## Repository

- GitHub repo: `https://github.com/TnTBass/signport` (private)
- Origin remote: `https://github.com/TnTBass/signport.git`
- Fork of `https://github.com/approved-mc-mods/signport`. Credit the original author as `approved`; homepage metadata points to `https://github.com/approved`.
- The local checkout lives at `C:\Users\tyler\AI Projects\signport`.
- This repo uses git worktrees under `.claude/worktrees/`. Each worktree has its own branch; the main checkout is on `main`.

## Product

SignPort is a Fabric Minecraft mod that lets players create named **anchors** and use signs as portals to those anchors.

Sign format:

```text
line 1: any text
line 2: [sp] or [signport]
line 3: anchor-name
line 4: optional dimension id (e.g. minecraft:the_nether)
```

Right-clicking a valid portal sign teleports the player to the named anchor. The `crossDimensionPortalSigns` config flag controls whether line 4 is honored.

### Commands

```text
/signport tp <name>
/signport anchor create <name>
/signport anchor create <name> <pos>
/signport anchor list
/signport anchor delete <name>
/signport anchor delete all
```

`/sp` is a redirect alias for `/signport`. Anchor management is gated behind `signport.anchor.*` permissions (default op level 2 via `protectedActionOpLevel` in config). Sign creation/edit/break are gated behind `signport.sign.*` permissions; sign use is open by default.

### Storage model

Anchors are stored as a single `SavedData` file on the **overworld**, not per dimension. Each `Anchor` record carries a `ResourceKey<Level>` so cross-dimension queries are scoped by dimension key. Reads use `getDataStorage().get(TYPE)` (returns nullable, wrap with `Optional.ofNullable`) — never `computeIfAbsent` for read-only access, because that registers the state and forces a save on shutdown even when no data was ever written.

The codec was migrated from a legacy per-dimension layout to the v2 overworld layout. The migration pattern: read both old and new fields with `optionalFieldOf`, promote legacy data on read, and call `setDirty()` so the next save rewrites in the new format. No separate version field needed. After deleting a migrated legacy file, attempt to delete its empty parent directories — catch and ignore `IOException` from non-empty dirs so other mods' files are left alone.

## Target Platform

| Item | Version |
| --- | --- |
| Minecraft | `26.1.2` |
| Java | `25` |
| Loader | `0.19.2` |
| Loom | `1.16.1` |
| Fabric API | `0.147.0+26.1.2` |
| fabric-permissions-api | `0.7.0` |

Mappings: **Mojang official**, not Yarn. The project was migrated from Yarn during the MC 26.1.2 port (May 2026). Confirmed Yarn → Mojang renames worth remembering:

- `player.sendMessage(Text, boolean)` → `player.sendSystemMessage(Component)` (chat) or `sendOverlayMessage(Component)` (hotbar; boolean `true` mapped to this).
- `player.hasPermissionLevel(int)` removed. Use `player.permissions() instanceof LevelBasedPermissionSet pls && pls.level().isEqualOrHigherThan(PermissionLevel.byId(n))`.
- `Identifier.of(ns, path)` → `Identifier.fromNamespaceAndPath(ns, path)`.
- `Identifier.tryParse("the_nether")` works; `Identifier.tryParse("nether")` produces `minecraft:nether` (NOT a real dimension). Aliases for `nether` → `the_nether` and `end` → `the_end` are in place.
- `PositionFlag` → `net.minecraft.world.entity.Relative`.
- `EntityPose` → `net.minecraft.world.entity.Pose`; `isInPose()` → `hasPose()`.
- `interactBlock` → `useItemOn` (ServerPlayerGameMode). `tryChangeText` → `updateSignText`; `changeText` → `updateText` (SignBlockEntity). `isPlayerFacingFront()` → `isFacingFrontText()`.
- `markDirty()` → `setDirty()` (SavedData).
- `getPersistentStateManager()` → `getDataStorage()`. `PersistentStateType` → `SavedDataType`. `PersistentState` → `SavedData`. `PersistentStateManager` → `SavedDataStorage`.
- `SavedDataStorage.get(SavedDataType<T>)` returns `T` (nullable), **not** `Optional<T>`. Wrap with `Optional.ofNullable()` at call sites.
- `BlockPos.add(x,y,z)` → `BlockPos.offset(x,y,z)`. `pos.up()` → `pos.above()`. `pos.down()` → `pos.below()`.
- `Vec3d.ofBottomCenter()` → `Vec3.atBottomCenterOf()`.
- `world.isClient()` → `level.isClientSide()` (method, not field).
- `world.getBottomY()` → `level.getMinY()`. `world.getTopYInclusive()` → `level.getMaxY()`.
- `state.isOf(Block)` → `state.is(Block)` (from `TypedInstance`).
- `state.isSideSolidFullSquare()` → `state.isFaceSturdy()`.
- `player.getBlockPos()` → `player.blockPosition()`.
- `server.getWorld(key)` → `server.getLevel(key)`. `RegistryKey.of()` → `ResourceKey.create()`.
- `ResourceKey.location()` does not exist in MC 26.1.2 Mojang mappings. Use `ResourceKey.identifier()` for the key id.

Use `javap` against the deobf jar to look up Mojang names faster than IntelliJ:

```powershell
$jar = (Get-ChildItem ".gradle/caches/fabric-loom/minecraftMaven/net/minecraft" -Recurse -Filter "minecraft-common-deobf-*.jar" | Select-Object -First 1).FullName
javap -classpath $jar net.minecraft.fully.qualified.ClassName
```

Loom 1.16.1 does NOT include the `migrateMappings` Gradle task. There is no automated migration available; manual edits only.

## Source Set Layout

`build.gradle` has `splitEnvironmentSourceSets()` enabled. The `client` sourceSet is currently empty — the mod ships server-and-client behavior in a single jar with `environment: "*"` in `fabric.mod.json`. The optional client mod features (described under "Intended Path") are where the `client` sourceSet first gets populated.

## Build and Test

Build command (Windows-friendly to avoid the 8191-char command-line limit — never pipe Gradle output directly through `Select-String`):

```powershell
.\gradlew.bat --quiet build > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED|cannot find symbol|incompatible types|mixin apply failed' | Select-Object -First 80
```

Use this quiet build pattern by default to conserve context. Do not inspect or paste the full Gradle log unless the compact error scan is insufficient.

The build attaches several gates to `check`:

- **`checkChangelog`** — releasable changes (anything under `src/main/`, `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/`, `.github/workflows/`, or `README.md`) must add a bullet under `## Unreleased` in either `CHANGELOG.md` for public player/admin-facing notes or `INTERNAL_CHANGELOG.md` for maintainer-only build/release/process notes. Dependabot actors/refs are skipped.
- **`checkWrapperChecksum`** — `gradle/wrapper/gradle-wrapper.properties` must pin `distributionSha256Sum`.
- **`checkUnitTestCompanions`** — every `*Config.java`, `*Format.java`, `*Resolver.java`, `*State.java` under `src/main/java` must have a matching `*Test.java` under `src/test/java`. New helper classes that match those suffixes need a companion test or the build fails.
- **`checkSourceHygiene`** — guards specific patterns (mixin shape, GitHub workflow permissions, release workflow secret usage, GitHub Actions pinned to commit SHAs not mutable tags, stale Mojang mapping accessors such as `ResourceKey.location()`). Read `build.gradle` before adding patterns to make sure the gate stays satisfied.

`compileJava` passes before `compileTestJava` runs. A clean `compileJava` does NOT mean the build is clean — always run the full build to catch test compile failures. Test source files (`src/test`) need the same package renames as main sources on any future mappings migration.

## GitHub and CI

- Use `gh` for pushes, run checks, Dependabot merges, and releases.
- After pushing a commit, check GitHub Actions.
- CI runs `build` on push and PR.
- Dependabot is enabled for Gradle and GitHub Actions. Refreshed Dependabot PRs may be merged when checks pass.
- GitHub Actions are pinned to commit SHAs (`checkSourceHygiene` enforces this). Do not introduce mutable `@v3`-style refs.

## Release Cadence

Work-in-progress phases do NOT trigger releases. Do not assume every phase needs its own commit. The per-phase rhythm is:

1. Build locally (the build is the primary mechanical gate).
2. Add bullets under `## Unreleased` in the right changelog: `CHANGELOG.md` for public player/admin-facing notes, or `INTERNAL_CHANGELOG.md` for maintainer-only build/release/process notes.

That's all that's required per phase unless the user asks for a commit. Prefer batching related phases together, then when the final phase in the batch is clean: build, commit, and prepare/release if explicitly requested. Pushing to GitHub and cutting a release are separate decisions.

- **Push:** whenever convenient. CI re-runs the gates on push.
- **Release:** an explicit decision made when one or more Unreleased entries are worth shipping together. A release does NOT have to map 1:1 to a phase — it's common to bundle multiple phases into a single versioned release.

The `releaseReminder` and `internalReleaseReminder` Gradle tasks that run at the end of `build` are informational; they list public and internal Unreleased entries separately. They do not release anything.

## Release Workflow

Version scheme: `<major>.<minor>.<patch>+mc<mc-version>` — e.g. `1.2.1+mc26.1.2`. Tags use `v<version>` — e.g. `v1.2.1+mc26.1.2`.

Release steps:

1. Move relevant public `CHANGELOG.md` entries from `Unreleased` to a versioned section. Keep maintainer-only entries in `INTERNAL_CHANGELOG.md`; they are not published to GitHub Releases, Modrinth, or CurseForge.
2. Update `mod_version` in `gradle.properties`.
3. Run the full build.
4. Commit the release prep.
5. Tag the release.
6. Push `main` and the tag.
7. Verify the release workflow run on GitHub Actions.

Before pushing release prep or tags, show the user the exact versioned public `CHANGELOG.md` section that will be published and wait for approval.

The release workflow validates the tag pattern, builds with Java 25, creates a GitHub Release with the remapped mod jar and sources jar, and publishes the same build to Modrinth (`signport` project) and CurseForge.

Required secrets:

| Secret | Purpose |
| --- | --- |
| `MODRINTH_TOKEN` | Creates versions for the Modrinth project. |
| `CURSEFORGE_TOKEN` | Uploads files via the CurseForge Upload API. |
| `CURSEFORGE_PROJECT_ID` | Numeric CurseForge project ID. |

## Permissions Policy

- Use `fabric-permissions-api`. LuckPerms is the intended server-side provider, but SignPort does not depend on LuckPerms directly.
- Defaults preserve sensible vanilla fallbacks via `protectedActionOpLevel` (default 2).

| Permission | Default |
| --- | --- |
| `signport.anchor.create` | op level 2 |
| `signport.anchor.delete` | op level 2 |
| `signport.anchor.list` | op level 2 |
| `signport.teleport.command` | everyone |
| `signport.sign.create` | op level 2 |
| `signport.sign.edit` | op level 2 |
| `signport.sign.break` | op level 2 |
| `signport.sign.use` | everyone |

## Configuration

`config/signport.json` is created on first server start. Existing fields:

| Field | Default | Purpose |
| --- | --- | --- |
| `teleportCommandDefault` | `true` | Vanilla fallback for `signport.teleport.command`. |
| `signUseDefault` | `true` | Vanilla fallback for `signport.sign.use`. |
| `protectedActionOpLevel` | `2` | Op level for protected anchor commands and portal sign create/edit/break fallbacks. |
| `crossDimensionPortalSigns` | `true` | Allows portal signs to reference anchors in other dimensions via line 4. |
| `safeTeleportSearch` | `true` | Searches for a safe standing position near an anchor; when false, teleports use the anchor block center directly. |

When adding new fields, follow the existing pattern in `SignPortConfig` and write a unit test — the `*Config.java` companion-test gate enforces this.

## Architectural Principles

When the user explicitly asks for input on an architectural change to one of these, raise the trade-off; do not silently re-litigate.

1. **Sign references are sacred.** Signs reference anchors by name. There is NO `/sp anchor rename` command and there will never be — renaming would silently break every sign pointing at the old name. Use a separate metadata field (`group`, etc.) for things that need to change.
2. **Vanilla clients must keep working for all server-side features.** Pagination, filtering, grouping, sort flags, BlueMap markers — all use only chat output, brigadier suggestions, or external systems. A vanilla client connecting to a SignPort server gets the full chat-based UX.
3. **The optional client mod degrades gracefully.** Server detects a SignPort-equipped client via channel handshake (`ServerPlayNetworking.canSend(...)`). Vanilla clients receive no payloads; SignPort-equipped clients connecting to a vanilla server detect the missing channel and disable client features that depend on the synced cache.
4. **Third-party config libraries are soft-deps only.** SignPort must load and run with no Cloth Config, no ModMenu, no BlueMap. Config values live in plain POJOs with JSON serialization; library-backed screens are an optional UI layer on top, gated by `FabricLoader.isModLoaded(...)` and class-existence guards.

## Intended Path (Workstream)

The current work is a UX overhaul for finding, organizing, and creating teleport signs. The motivating problem: as servers accumulate anchors, `/sp anchor list` becomes a wall of chat lines, and players don't know which names are valid when placing a sign.

The work is broken into seven phases. Detailed phase prompts live in `prompts/` (gitignored — local working notes, not committed). On a fresh clone, ask the user for the relevant phase prompt before starting a phase.

| Phase | Scope | Schema change | Client work |
| --- | --- | --- | --- |
| 1 | Tab-completion on anchor names; `/sp anchor list [page]` pagination; substring filter argument | No | No |
| 2 | Anchor groups (separate `group` metadata field). `/sp anchor create <name> [group]`, `/sp anchor setgroup <name> <group>`, group headers in list output | Yes (optional `group`) | No |
| 3 | `/sp anchor near [radius]` with default radius from config; `--sort=name\|distance\|recent` flag on list | Yes (optional `createdAt`) | No |
| 4 | BlueMap integration as soft-dep, server-only. One marker set per dimension; click runs `/sp tp <name>` | No | No |
| 5 | Client mod foundation. S2C anchor sync packet (full dump on login + delta on create/delete/setgroup). Permission sync. Cloth Config + ModMenu as soft-deps. First feature: HUD lookup hint when looking at a port sign | No | Yes |
| 6 | Keybind-launched anchor browser screen (search, sort, group tabs); sign-editor autocomplete for anchor names on line 3 | No | Yes |
| 7 | Permission-gated "SignPort Template" button injected into the vanilla sign-editing screen; opens a small form that writes the four sign lines | No | Yes |

**Phase dependencies:**

```
Phase 1 ─→ Phase 2 ─→ Phase 3
Phase 4 (fully independent — parallelizable with anything)
Phase 5 ─→ Phase 6
        └→ Phase 7
```

Phase 5 has a soft dependency on Phase 2 (the synced payload includes the `group` field).

**Explicitly dropped during planning (do not reintroduce without discussion):**

- `/`-in-name grouping — replaced by the separate `group` field in Phase 2.
- `/sp anchor rename` — would silently corrupt every sign.
- "Nearby anchors" HUD widget and waypoint beams — overlap with the browser screen and add visual noise.
- Xaero / JourneyMap waypoint export — BlueMap is a better fit and replaces it.

## Client-Side Framework Choices

The user's reference modset:

- **Server:** BlueMap 5.20, Cloth Config 26.1.154, Architectury 19.0.1, fabric-permissions-api, LuckPerms, plus performance and gameplay mods.
- **Client:** Cloth Config 26.1.154, Sodium, Lithium, malilib, minihud. **No ModMenu.**

Resulting framework picks for the client mod phases:

| Concern | Choice | Rationale |
| --- | --- | --- |
| Networking | Fabric API `ClientPlayNetworking` + `PayloadTypeRegistry` | Already on classpath via Fabric API. |
| Keybinds | Fabric API `KeyBindingHelper` (`fabric-key-binding-api-v1`) via runtime reflection if the split client source set cannot compile against the remapped helper directly | Standard, surfaces in vanilla Controls menu. This repo's Loom setup does not expose `modCompileOnly` / `modImplementation`; a raw client dependency can leak intermediary names such as `class_304`. |
| HUD overlay | Client tick + `LocalPlayer.sendOverlayMessage(...)` | Matches the vanilla action-bar location and avoids depending on the newer HUD extraction API shape. |
| Screens | Vanilla `Screen` widgets | Sufficient. No `owo-lib` or `malilib` dependency. |
| Mixin | Loader's bundled Mixin + MixinExtras | Already on classpath. |
| Client config values | Plain POJO + JSON, always loaded | Must work without any third-party config library installed. |
| Client config screen | **Cloth Config** as a **soft-dep** with class-existence guard | Already in user's modset both sides. Not YACL — would add a dep the user doesn't have. If Cloth Config is absent, SignPort still loads; the settings screen is unavailable and users edit `config/signport-client.json` directly. |
| Config screen discoverability | **ModMenu as optional soft-dep** + always-registered (default unbound) keybind | User's client modset does NOT include ModMenu. Soft-dep gives ModMenu users a familiar entry; the keybind is the fallback for non-ModMenu users. |
| BlueMap (Phase 4) | `de.bluecolored:bluemap-api:2.7.x` via `https://repo.bluecolored.de/releases` | User's server runs BlueMap 5.20. Compile-only soft-dep with class-existence guard. The BlueMap product version is 5.x, but the public API artifact is versioned 2.7.x. |
| Multiloader abstractions | None | SignPort stays Fabric-only. |

## Phase Implementation Notes

- BlueMap 5.x uses the `de.bluecolored:bluemap-api:2.7.x` Maven artifact; do not look for a `bluemap-api:5.x` artifact.
- BlueMap POI markers support popup detail HTML in the public API. As of API 2.7.6, they do not expose a server-command click handler, so SignPort markers should show `/sp tp <name>` in marker detail unless a newer API adds command support.
- This Loom 1.16.1 setup does not expose `modCompileOnly` or `modImplementation`. Use `compileOnly` / `clientCompileOnly` for optional APIs such as Cloth Config and ModMenu, and add explicit `clientImplementation` only when the split `client` source set needs the aggregate Fabric API on its compile classpath.
- Keep optional third-party integrations split between a loader-safe entry class and a bridge class that alone imports optional API types. The entry class should guard with `FabricLoader.isModLoaded(...)` and config before loading the bridge.
- Register custom payload types through a shared idempotent helper. In integrated/client runs, common and client initializers can execute in the same JVM, so duplicate `PayloadTypeRegistry.clientboundPlay().register(...)` calls are risky.
- For HUD lookup hints, cache the resolved target and sign-text key, not the fully rendered message. Recompute only when the looked-at sign or text changes, but format dynamic values such as distance every tick so the display stays accurate.
- For optional config keybinds, add a lang entry under `src/main/resources/assets/signport/lang/en_us.json`; otherwise Controls shows the raw translation key.
- For future phase work, start with `rg --files` and `rg -n`, then use targeted range reads. Avoid full-file reads of large command or state classes unless the change touches broad behavior.
- When the worktree is already dirty, record the baseline with `git status --short` and keep review diffs scoped to files touched in the current phase.

## Working Style

- Keep phases focused. One phase per session unless a previous phase is already complete and committed.
- Update `CHANGELOG.md` under `## Unreleased` for public player/admin-facing phase changes, or `INTERNAL_CHANGELOG.md` under `## Unreleased` for maintainer-only build/release/process changes — the `checkChangelog` gate enforces this split. Do not commit every phase by default; batch commits until the final requested phase or until the user explicitly asks for a checkpoint commit.
- Do not push or tag at the end of a phase. Those are explicit user decisions; see "Release Cadence" above.
- Do not revert unrelated user changes.
- Prefer small, testable helpers; the `checkUnitTestCompanions` gate enforces companion tests for `*Config`, `*Format`, `*Resolver`, `*State` classes.
- Keep `README.md` aligned with user-visible behavior.
- Be token-efficient: use `rg` and targeted file reads, prefer `git diff --stat` before full diffs, and run focused tests while iterating before one quiet full build at the end.
- For phase prompts, turn nuanced requirements into explicit invariants before editing. Example: if sorted list output still needs group headers, decide whether rows must be regrouped after sorting so headers appear once in first-occurrence order.
- When locating prompt files or repo context, index first (`rg --files`, `rg -n`) and range-read second. Avoid dumping large files after the project shape is known; read only the relevant sections unless the user explicitly asks for a full-file read.
- If a prompt's "done when" conflicts with the user's latest instructions, follow the user's latest instructions and mention the conflict briefly.
- For exploratory questions, give a 2–3 sentence recommendation with the main trade-off. Do not implement until the user agrees.

## File Layout (relevant entries)

```
src/main/java/tech/endorsed/signport/
  SignPort.java                       (main entrypoint)
  command/AnchorCommand.java          (/sp commands — Phases 1, 2, 3 touch this)
  config/SignPortConfig.java          (config surface — Phases 1, 3, 4, 5 add fields)
  events/SignEvents.java              (sign break protection)
  mixin/                              (server-side mixins)
  permission/SignPortPermissions.java (permission helpers)
  world/
    Anchor.java                       (record — Phase 2 adds group, Phase 3 adds createdAt)
    AnchorState.java                  (SavedData — Phases 2, 3 codec changes)
    TeleportDestinationResolver.java  (anchor → safe position; Phase 5 mirrors lookup logic client-side)
src/client/java/...                   (currently empty; Phase 5 first populates it)
src/main/resources/
  fabric.mod.json
  signport.mixins.json
src/test/java/...                     (companion tests — required for *Config/*Format/*Resolver/*State)
prompts/                              (gitignored — local phase prompts and working notes)
.github/workflows/                    (build.yml, release.yml — pinned to commit SHAs)
AGENTS.md                             (this file — canonical agent handoff)
```
