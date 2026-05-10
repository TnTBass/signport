# SignPort

SignPort is a Fabric mod that turns ordinary signs into named travel portals. Create an anchor, write its name on a sign, and right-click the sign to teleport.

Original project by [approved (GitHub)](https://github.com/approved) - [approved (Modrinth)](https://modrinth.com/user/approved). Posted with their express permission.

## Features

- Create, list, delete, and teleport to named anchors.
- Turn signs into portals with `[sp]` or `[signport]`.
- Target anchors in the current dimension or another dimension.
- Supports front/back sign text by checking the side the player is facing first.
- Colors the portal marker green for valid targets and red for missing targets.
- Supports LuckPerms through `fabric-permissions-api`, with vanilla operator fallbacks.

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
- Line 4: the_nether

## Commands

- `/signport anchor create (name)`
- `/signport anchor create (name) (x) (y) (z)`
- `/signport anchor list`
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

## Requirements

- Minecraft 26.1.2
- Fabric Loader
- Fabric API
- Java 25

Install SignPort on the server. Players do not need to install it on their clients.

## Modpacks and Redistribution

You may include SignPort in modpacks. If you redistribute the jar directly, keep the LGPL-3.0-only license notice, credit the original project and author, and link back to the [GitHub source](https://github.com/TnTBass/signport). Modified versions must follow the LGPL-3.0-only license terms.
