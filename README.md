# SignPort

A Fabric sign portal mod for quick travel between named anchors.

Original project by [approved](https://github.com/approved).

## Commands

Anchor management requires operator permission level 2.

```text
/signport anchor create <name>
/signport anchor create <name> <group>
/signport anchor create <name> <x> <y> <z>
/signport anchor setgroup <name> <group>
/signport anchor list [filter] [page] [--sort=name|distance|recent]
/signport anchor near [radius] [page]
/signport anchor delete <name>
/signport anchor delete all
```

Players can teleport to an anchor by name:

```text
/signport tp <name>
```

The shorter `/sp` alias can be used in place of `/signport`.

`/sp anchor create <name> <group>` stores the group as metadata; portal signs still reference only `<name>`. `/sp anchor setgroup <name> <group>` moves an existing anchor between groups without changing the sign-facing anchor name. Use `-` or `""` as the group to move an anchor back to `(ungrouped)`.

`/sp anchor list` is paginated (page size configurable via `anchorListPageSize`, default 10). The optional `filter` is a case-insensitive substring match on anchor name. By default anchors are sorted by group, then name, with group headers and counts shown above the rows on each page. Add `--sort=distance` to sort anchors in the current dimension by distance from you and show a distance column, or `--sort=recent` to show newly created anchors first. The footer's `[« Prev]` and `[Next »]` brackets are clickable to navigate pages and preserve the active filter and sort.

`/sp anchor near [radius]` lists anchors in your current dimension within the given radius, sorted nearest first and grouped like the main list. If `radius` is omitted, SignPort uses `defaultNearRadius`.

`/sp tp <name>`, `/sp anchor delete <name>`, and `/sp anchor setgroup <name>` tab-complete from anchors in the player's current dimension; group arguments suggest existing groups.

## BlueMap Integration

When BlueMap is installed on the server, SignPort publishes anchors as POI markers on each BlueMap world map. Markers are grouped under "SignPort Anchors", show the anchor group (or `ungrouped`) and dimension in the popup, and include the in-game teleport command `/sp tp <name>`.

BlueMap is optional. When BlueMap is installed, SignPort publishes anchors as marker sets by dimension. This is enabled by default; set `bluemapEnabled` to `false` in `config/signport.json` to disable marker registration while keeping BlueMap installed.

## Optional Client Features

The same SignPort jar also includes an optional Fabric client mod. Vanilla clients still work normally: servers only send SignPort client payloads to clients that advertise the channel. When both sides have SignPort, the client receives anchor and permission sync data for the features below.

- Looking at a portal sign shows a small hotbar lookup hint for the resolved anchor.
- Press `J` to open the SignPort anchor browser. It has dimension tabs, search, name/distance/recent sorting, collapsible group sections, and click-to-run `/sp tp <name>` rows. Players with anchor delete permission also see a raw `/tp @s <x> <y> <z>` shortcut.
- While editing a portal sign, line 3 autocompletes anchor names from the synced cache. If line 4 contains a dimension id, suggestions are scoped to that dimension; otherwise suggestions include their dimension in parentheses.

### Sign template form

Players with `signport.sign.create` permission see a **SignPort Template** button while editing a sign on a SignPort-equipped server. The form accepts a target anchor, an optional dimension from the synced anchor cache, and an optional label for line 1, then fills the sign as `[sp]` plus the selected anchor details. Players can still edit the sign lines manually before pressing Done. The button is hidden when permission sync is unavailable, when the server reports the player cannot create portal signs, or when `signTemplateButtonEnabled` is disabled.

Client settings live in `config/signport-client.json`. `hudHintEnabled`, `browserKeybindEnabled`, `signEditorAutocompleteEnabled`, and `signTemplateButtonEnabled` all default to `true`; disabling the browser keybind takes effect after restarting the game. If Cloth Config is installed, the settings screen can be opened through the default-unbound `key.signport.config` keybind. ModMenu users also get a SignPort settings entry when ModMenu and Cloth Config are both present.

The settings screen also shows passive SignPort client/server status when the server advertises the status payload. `Unknown`, `Disconnected`, and `Server not detected` are informational states; vanilla clients, vanilla servers, and servers without the status payload continue to work normally.

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

On first server start, SignPort creates `config/signport.json` with the default server values.

| Option | Default | Description |
| --- | --- | --- |
| `teleportCommandDefault` | `true` | Vanilla fallback for `signport.teleport.command`; `true` allows everyone when no permission provider overrides it. |
| `signUseDefault` | `true` | Vanilla fallback for `signport.sign.use`; `true` allows everyone when no permission provider overrides it. |
| `protectedActionOpLevel` | `2` | Operator level used for protected anchor commands, portal sign create/edit/break fallbacks, and anchor-list teleport links. |
| `crossDimensionPortalSigns` | `true` | Allows portal signs to use line 4 as a dimension id when the anchor is not found in the current world. |
| `safeTeleportSearch` | `true` | Searches for a safe standing position near an anchor before teleporting. When disabled, teleports use the anchor block center directly. |
| `anchorListPageSize` | `10` | Number of anchors shown per page in `/sp anchor list`. Must be between 1 and 100. |
| `defaultNearRadius` | `128` | Radius used by `/sp anchor near` when no radius is provided. Must be between 1 and 10000. |
| `bluemapEnabled` | `true` | Enables SignPort anchor markers when BlueMap is installed. |

When installed client-side, SignPort also creates `config/signport-client.json` with these optional client feature toggles:

| Option | Default | Description |
| --- | --- | --- |
| `hudHintEnabled` | `true` | Shows hotbar lookup hints when looking at portal signs. |
| `browserKeybindEnabled` | `true` | Enables the anchor browser keybind. Restart the game after changing this value. |
| `signEditorAutocompleteEnabled` | `true` | Enables anchor-name autocomplete while editing portal signs. |
| `signTemplateButtonEnabled` | `true` | Shows the SignPort Template button when the server reports that the player can create portal signs. |

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
| `MODRINTH_TOKEN` | Creates versions for the `signport` Modrinth project. |
| `CURSEFORGE_TOKEN` | Uploads files through the CurseForge Upload API. |
| `CURSEFORGE_PROJECT_ID` | Numeric CurseForge project ID for the SignPort project. |
