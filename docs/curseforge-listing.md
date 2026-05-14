# SignPort

SignPort is a Fabric mod that turns ordinary signs into named travel portals. Create an anchor, write its name on a sign, and right-click the sign to teleport.

Original project by [approved (GitHub)](https://github.com/approved) - [approved (Modrinth)](https://modrinth.com/user/approved). Posted with their express permission.

## Features

- Create, group, list, filter, sort, delete, and teleport to named anchors.
- Turn signs into portals with `[sp]` or `[signport]`.
- Target anchors in the current dimension or another dimension.
- Browse nearby anchors with `/sp anchor near [radius]`.
- Use tab completion for anchor names and groups.
- Supports front/back sign text by checking the side the player is facing first.
- Colors the portal marker green for valid targets and red for missing targets.
- Optional client features add HUD lookup hints, an anchor browser, sign-editor autocomplete, and a SignPort Template form for filling valid portal signs.
- Optional BlueMap integration publishes anchor markers to your web map.
- Supports LuckPerms through `fabric-permissions-api`, with vanilla operator fallbacks.

## Optional Integrations

SignPort's core portal and command features are server-side. Vanilla clients can join and use portal signs normally.

- **SignPort client mod:** optional on players' clients. When both client and server have SignPort installed, the client receives synced anchor and permission data. This enables portal-sign HUD lookup hints, an anchor browser opened with the `J` key, sign-editor autocomplete, and the permission-gated SignPort Template button.
- **BlueMap:** optional server-side integration. If BlueMap is installed, SignPort publishes anchors as marker sets by dimension by default. Server owners can disable this with `bluemapEnabled=false` in `config/signport.json`.
- **LuckPerms or another `fabric-permissions-api` provider:** optional server-side permission provider. Without one, SignPort uses vanilla operator fallback levels.
- **Cloth Config:** optional client-side config screen. If installed, SignPort's client settings can be edited in-game.
- **ModMenu:** optional client-side settings entry. If ModMenu and Cloth Config are both installed, SignPort appears in ModMenu's config list.

## Sign Format

- **Line 1:** Any text (display label)
- **Line 2:** `[sp]` or `[signport]`
- **Line 3:** Anchor name
- **Line 4:** Dimension ID (optional)

**Spawn example:**

- Line 1: Spawn
- Line 2: `[sp]`
- Line 3: spawn

**Cross-dimension example:**

- Line 1: Nether Hub
- Line 2: `[signport]`
- Line 3: hub
- Line 4: nether

Accepted dimension values for line 4: `overworld`, `nether`, `end`, `the_nether`, `the_end`, or any full namespace ID like `minecraft:overworld` or `mymod:custom_dimension`.

## Commands

- `/signport anchor create (name)`
- `/signport anchor create (name) (group)`
- `/signport anchor create (name) (x) (y) (z)`
- `/signport anchor setgroup (name) (group)`
- `/signport anchor list [filter] [page] [--sort=name|distance|recent]`
- `/signport anchor near [radius] [page]`
- `/signport anchor delete (name)`
- `/signport anchor delete all`
- `/signport tp (name)`

The shorter `/sp` alias can be used in place of `/signport`.

## Permissions

SignPort works with LuckPerms through `fabric-permissions-api`. If no permission provider is installed, it falls back to vanilla operator checks.

- `signport.anchor.create` — operator level 2
- `signport.anchor.delete` — operator level 2
- `signport.anchor.list` — operator level 2
- `signport.teleport.command` — everyone
- `signport.sign.create` — operator level 2
- `signport.sign.edit` — operator level 2
- `signport.sign.break` — operator level 2
- `signport.sign.use` — everyone

## Configuration

On first server start, SignPort creates `config/signport.json` with these server settings:

- `teleportCommandDefault` — default `true`; allows everyone to use `/sp tp` when no permission provider overrides it.
- `signUseDefault` — default `true`; allows everyone to use portal signs when no permission provider overrides it.
- `protectedActionOpLevel` — default `2`; vanilla op level fallback for protected anchor and portal-sign actions.
- `crossDimensionPortalSigns` — default `true`; lets line 4 on signs target another dimension.
- `safeTeleportSearch` — default `true`; searches for a safe nearby standing spot before teleporting.
- `anchorListPageSize` — default `10`; number of anchors shown per `/sp anchor list` page.
- `defaultNearRadius` — default `128`; default radius for `/sp anchor near`.
- `bluemapEnabled` — default `true`; enables BlueMap anchor markers when BlueMap is installed.

When installed client-side, SignPort creates `config/signport-client.json` with these client settings:

- `hudHintEnabled` — default `true`; shows portal-sign lookup hints in the hotbar.
- `browserKeybindEnabled` — default `true`; enables the anchor browser keybind. Restart the game after changing this value.
- `signEditorAutocompleteEnabled` — default `true`; enables anchor-name autocomplete while editing signs.
- `signTemplateButtonEnabled` — default `true`; shows the SignPort Template button when the server reports that the player can create portal signs.

## Requirements

- Fabric Loader
- Fabric API
- Java 25

Install SignPort on the server. Players do not need to install it on their clients, but installing the same jar client-side enables the optional client helpers listed above.

## Modpacks and Redistribution

You may include SignPort in modpacks. If you redistribute the jar directly, keep the LGPL-3.0-only license notice, credit the original project and author, and link back to the [GitHub source](https://github.com/TnTBass/signport). Modified versions must follow the LGPL-3.0-only license terms.
