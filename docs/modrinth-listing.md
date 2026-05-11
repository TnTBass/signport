# Modrinth Listing Draft

Use this as the working copy for the SignPort Modrinth project page.

## Project Fields

| Field | Value |
| --- | --- |
| Project type | Mod |
| Name | SignPort |
| Slug | `modern-signport` |
| Summary | Create named anchors and use signs as portals for fast Minecraft travel. |
| License | LGPL-3.0-only |
| Client side | Unsupported |
| Server side | Required |
| Minecraft versions | `26.1.2` |
| Loaders | Fabric |
| Categories | Utility, Transportation |
| Source URL | `https://github.com/TnTBass/signport` |
| Homepage URL | `https://github.com/approved` |
| Issues URL | `https://github.com/TnTBass/signport/issues` |
| Discord/Donation/Wiki | Leave blank unless public links are created. |

## Required Dependencies

- Fabric API

`fabric-permissions-api` is included in the built jar. LuckPerms is supported as the intended permission provider, but SignPort falls back to vanilla operator checks when no permission provider is installed.

## Icon

Use `docs/modrinth-icon.jpg` for the Modrinth project icon.

Modrinth project icons must be smaller than 256 KiB. Keep `docs/modrinth-icon-source.png` as the editable source image and export an optimized 512x512 icon for upload.

## Gallery Candidates

| File | Title | Caption |
| --- | --- | --- |
| `docs/modrinth-nether-portal-sign.png` | Nether Portal Sign | A SignPort sign using the short `the_nether` dimension form beside a Nether portal. |
| `docs/modrinth-portal-wall.png` | Portal Wall | A wall of SignPort destinations for quick travel around a world. |
| `docs/modrinth-nether-destination-signs.png` | Nether Destination Signs | Paired signs in the Nether linking back to the overworld and other hubs. |
| `docs/active_sign.png` | Active Portal Sign | A valid SignPort sign targeting a named anchor. |
| `docs/created_anchor.png` | Anchor Created | Anchors can be created and managed with `/signport` or `/sp`. |
| `docs/nether_to_overworld.png` | Cross-Dimension Travel | Optional dimension IDs let signs target anchors in another dimension. |
| `docs/missing_portal.png` | Missing Anchor Feedback | Invalid portal targets are marked red instead of silently failing. |
| `docs/missing_portal_to_nether.png` | Invalid Nether Target | Missing cross-dimension targets are shown clearly on the sign. |

## Project Description

````markdown
# SignPort

SignPort is a Fabric mod that turns ordinary signs into named travel portals. Create an anchor, write its name on a sign, and right-click the sign to teleport.

Original project by [approved (GitHub)](https://github.com/approved) - [approved (Modrinth)](https://modrinth.com/user/approved). Posted this project with their express permission.

## Features

- Create, list, delete, and teleport to named anchors.
- Turn signs into portals with `[sp]` or `[signport]`.
- Target anchors in the current dimension or another dimension.
- Supports front/back sign text by checking the side the player is facing first.
- Colors the portal marker green for valid targets and red for missing targets.
- Supports LuckPerms through `fabric-permissions-api`, with vanilla operator fallbacks.

## Sign Format

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
nether
```

Accepted dimension values for line 4: `overworld`, `nether`, `end`, `the_nether`, `the_end`, or any full namespace ID like `minecraft:overworld` or `mymod:custom_dimension`.

## Commands

```text
/signport anchor create <name>
/signport anchor create <name> <x> <y> <z>
/signport anchor list
/signport anchor delete <name>
/signport anchor delete all
/signport tp <name>
```

The shorter `/sp` alias can be used in place of `/signport`.

## Permissions

SignPort works with LuckPerms through `fabric-permissions-api`. If no permission provider is installed, it falls back to vanilla operator checks.

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

## Requirements

- Fabric Loader
- Fabric API
- Java 25

Install SignPort on the server. Players do not need to install it on their clients.

## Modpacks and Redistribution

You may include SignPort in modpacks.

For Modrinth modpacks, prefer adding SignPort as a project dependency so users download the official file from this page.

If you redistribute the jar directly outside Modrinth, keep the LGPL-3.0-only license notice, credit the original project/author attribution, and link back to this project or the GitHub source. Modified versions must follow the LGPL-3.0-only license terms.
````

## Initial Version Upload

| Field | Value |
| --- | --- |
| Version name | SignPort 1.1.0 for Minecraft 26.1.2 |
| Version number | `1.1.0+mc26.1.2` |
| Release channel | Release |
| Game versions | `26.1.2` |
| Loaders | Fabric |
| Primary file | GitHub Release asset `signport-1.1.0+mc26.1.2.jar` |
| Additional file | Do not upload sources jar to Modrinth. Sources are available through GitHub. |
| Required dependency | Fabric API |

Version changelog:

```markdown
- Added LuckPerms-compatible permission checks through `fabric-permissions-api`.
- Added permission nodes for anchor commands, portal sign creation/editing/breaking, portal sign use, and `/signport tp`.
```

## Pre-Submit Checklist

- Confirm the latest `.\gradlew.bat build` passes.
- Upload the project as a draft first.
- Attach the icon from `docs/modrinth-icon.jpg`.
- Add `docs/modrinth-nether-portal-sign.png` as the first gallery image.
- Add `docs/modrinth-portal-wall.png` or `docs/modrinth-nether-destination-signs.png` as supporting gallery images.
- Upload only the `1.1.0+mc26.1.2` mod jar. Do not upload the sources jar to Modrinth.
- Confirm Fabric API is marked as a required dependency.
- Confirm Client side is `Unsupported` and Server side is `Required` in the Modrinth UI.
- Confirm the source and issue links point to `https://github.com/TnTBass/signport`.
- Confirm the page description still credits the original project by `approved`.
