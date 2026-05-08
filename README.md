# SignPort

A Fabric sign portal mod for quick travel between named anchors.

Original project by [approved](https://github.com/approved).

## Commands

Anchor management requires operator permission level 2.

```text
/signport anchor create <name>
/signport anchor create <name> <x> <y> <z>
/signport anchor list
/signport anchor delete <name>
/signport anchor delete all
```

Players can teleport to an anchor by name:

```text
/signport tp <name>
```

The shorter `/sp` alias can be used in place of `/signport`.

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

