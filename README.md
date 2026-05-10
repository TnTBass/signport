# SignPort

A Fabric sign portal mod for quick travel between named anchors.

Original project by [approved](https://github.com/approved).

## Commands

Anchor management requires operator permission level 2.

```text
/signport anchor create <name>
/signport anchor create <name> <x> <y> <z>
/signport anchor list [filter] [page]
/signport anchor delete <name>
/signport anchor delete all
```

Players can teleport to an anchor by name:

```text
/signport tp <name>
```

The shorter `/sp` alias can be used in place of `/signport`.

`/sp anchor list` is paginated (page size configurable via `anchorListPageSize`, default 10). The optional `filter` is a case-insensitive substring match on anchor name. The footer's `[« Prev]` and `[Next »]` brackets are clickable to navigate pages and preserve the active filter. `/sp tp <name>` and `/sp anchor delete <name>` tab-complete from anchors in the player's current dimension.

## Portal Signs

A sign becomes a SignPort portal when one side uses this format:

```text
line 1: any text
line 2: [sp] or [signport]
line 3: anchor-name
line 4: optional dimension id
```

Examples:

```text
Spawn
[sp]
spawn
```

```text
Nether Hub
[signport]
hub
minecraft:the_nether
```

Right-clicking a valid portal sign teleports the player to the named anchor. If the side facing the player is not a valid portal, SignPort checks the other side before falling back to normal sign interaction.

Teleport destinations are resolved to a safe standing position at or near the anchor. SignPort centers the player horizontally on the destination block, avoids solid or harmful foot/head spaces, and cancels the teleport with a message if no nearby safe position can be found.

When a portal sign is edited, the portal marker line is colored green if the target anchor can be found and red if it cannot.

## Permissions

SignPort supports LuckPerms through `fabric-permissions-api`. If no permission provider is installed, SignPort falls back to vanilla operator checks.

| Permission | Default |
| --- | --- |
| `signport.anchor.create` | operator level 2 |
| `signport.anchor.delete` | operator level 2 |
| `signport.anchor.list` | operator level 2 |
| `signport.teleport.command` | everyone |
| `signport.sign.create` | operator level 2 |
| `signport.sign.edit` | operator level 2 |
| `signport.sign.break` | operator level 2 |
| `signport.sign.use` | everyone |

## Configuration

On first server start, SignPort creates `config/signport.json` with the default policy values.

| Option | Default | Description |
| --- | --- | --- |
| `teleportCommandDefault` | `true` | Vanilla fallback for `signport.teleport.command`; `true` allows everyone when no permission provider overrides it. |
| `signUseDefault` | `true` | Vanilla fallback for `signport.sign.use`; `true` allows everyone when no permission provider overrides it. |
| `protectedActionOpLevel` | `2` | Operator level used for protected anchor commands, portal sign create/edit/break fallbacks, and anchor-list teleport links. |
| `crossDimensionPortalSigns` | `true` | Allows portal signs to use line 4 as a dimension id when the anchor is not found in the current world. |
| `safeTeleportSearch` | `true` | Searches for a safe standing position near an anchor before teleporting. When disabled, teleports use the anchor block center directly. |
| `anchorListPageSize` | `10` | Number of anchors shown per page in `/sp anchor list`. Must be between 1 and 100. |

## Releases

Release tags use `v<version>`, where the version includes the Minecraft target, for example `v1.1.0+mc26.1.2`.

To publish a release, move the relevant changelog entries into a versioned section, update `mod_version` in `gradle.properties`, run `.\gradlew.bat build`, commit the release prep, tag the commit, and push `main` with the tag:

```powershell
git tag v1.1.0+mc26.1.2
git push origin main v1.1.0+mc26.1.2
```

Pushing the tag runs the release workflow. It builds with Java 25, creates a GitHub Release, attaches the remapped mod jar and sources jar from `build/libs`, and publishes the same build to Modrinth and CurseForge.

The workflow requires these repository secrets:

| Secret | Purpose |
| --- | --- |
| `MODRINTH_TOKEN` | Creates versions for the `modern-signport` Modrinth project. |
| `CURSEFORGE_TOKEN` | Uploads files through the CurseForge Upload API. |
| `CURSEFORGE_PROJECT_ID` | Numeric CurseForge project ID for the SignPort project. |
