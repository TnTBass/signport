# In-Game Validation

Use this checklist to validate SignPort on a real Fabric server. The focus is permission behavior through `fabric-permissions-api` with LuckPerms as the permission provider.

## Test Matrix

Target versions:

- Minecraft `1.21.10`
- Java `25`
- SignPort build from this repository
- Fabric API compatible with the project dependencies
- `fabric-permissions-api`
- LuckPerms for Fabric

Permission defaults implemented by SignPort:

| Permission | Vanilla fallback default |
| --- | --- |
| `signport.anchor.create` | operator level 2 |
| `signport.anchor.delete` | operator level 2 |
| `signport.anchor.list` | operator level 2 |
| `signport.teleport.command` | everyone |
| `signport.sign.create` | operator level 2 |
| `signport.sign.edit` | operator level 2 |
| `signport.sign.break` | operator level 2 |
| `signport.sign.use` | everyone |

## Setup

1. Build the mod with `.\gradlew.bat build`.
2. Install the generated SignPort jar, Fabric API, `fabric-permissions-api`, and LuckPerms in a Fabric server.
3. Start the server and confirm it reaches the player login state without SignPort, Fabric API, `fabric-permissions-api`, or LuckPerms errors.
4. Join with two test accounts:
   - `OpPlayer`: operator level 2 or higher.
   - `DefaultPlayer`: no operator status and no LuckPerms grants.
5. Create at least one anchor as `OpPlayer`:

```text
/signport anchor create spawn
```

## Manual Checklist

| ID | Scenario | Steps | Expected result | Status |
| --- | --- | --- | --- | --- |
| V-001 | Server starts with permission stack | Start the server with SignPort, Fabric API, `fabric-permissions-api`, and LuckPerms installed. | Server starts cleanly and both players can join. | Manual |
| V-002 | Operator can create anchors | As `OpPlayer`, run `/signport anchor create op_anchor`. | Command succeeds and reports `Created anchor 'op_anchor'`. | Manual |
| V-003 | Operator can list anchors | As `OpPlayer`, run `/signport anchor list`. | Anchor list is shown. Anchor entries are clickable for vanilla `/tp` because the player is op level 2. | Manual |
| V-004 | Operator can delete anchors | As `OpPlayer`, run `/signport anchor delete op_anchor`. | Command succeeds and reports `Deleted anchor 'op_anchor'`. | Manual |
| V-005 | Default player cannot create anchors | As `DefaultPlayer`, run `/signport anchor create denied_anchor`. | Command is unavailable or rejected by command permission checks. No anchor is created. | Manual |
| V-006 | Default player cannot list anchors | As `DefaultPlayer`, run `/signport anchor list`. | Command is unavailable or rejected by command permission checks. | Manual |
| V-007 | Default player cannot delete anchors | As `DefaultPlayer`, run `/signport anchor delete spawn`. | Command is unavailable or rejected by command permission checks. Anchor remains. | Manual |
| V-008 | Default player can use valid portal signs | As `OpPlayer`, create a sign with line 2 `[sp]` and line 3 `spawn`. As `DefaultPlayer`, right-click it. | `DefaultPlayer` teleports to the `spawn` anchor. | Manual |
| V-009 | Default player cannot create portal signs | As `DefaultPlayer`, try to write a non-portal sign into a portal sign with line 2 `[sp]`. | Edit is cancelled, the player sees `You do not have permissions to create port signs.`, and the sign does not become a portal. | Manual |
| V-010 | Default player cannot edit portal signs | As `DefaultPlayer`, try to edit an existing portal sign. | Edit is cancelled and the player sees `You do not have permissions to edit port signs.` | Manual |
| V-011 | Default player cannot break portal signs | As `DefaultPlayer`, try to break a valid portal sign. | Break is cancelled, the sign remains, and the player sees `You do not have permissions to remove port signs.` | Manual |
| V-012 | LuckPerms grant allows anchor create | Grant `DefaultPlayer` `signport.anchor.create`, then run `/signport anchor create lp_create`. | Command succeeds. Revoke the grant after the check. | Manual |
| V-013 | LuckPerms grant allows anchor list | Grant `DefaultPlayer` `signport.anchor.list`, then run `/signport anchor list`. | Command succeeds. Revoke the grant after the check. | Manual |
| V-014 | LuckPerms grant allows anchor delete | Create `lp_delete` as `OpPlayer`. Grant `DefaultPlayer` `signport.anchor.delete`, then run `/signport anchor delete lp_delete`. | Command succeeds. Revoke the grant after the check. | Manual |
| V-015 | LuckPerms grant allows portal sign create | Grant `DefaultPlayer` `signport.sign.create`, then create a sign with line 2 `[sp]` and line 3 `spawn`. | Portal sign creation succeeds. Revoke the grant after the check. | Manual |
| V-016 | LuckPerms grant allows portal sign edit | Grant `DefaultPlayer` `signport.sign.edit`, then edit an existing portal sign. | Portal sign edit succeeds. Revoke the grant after the check. | Manual |
| V-017 | LuckPerms grant allows portal sign break | Grant `DefaultPlayer` `signport.sign.break`, then break a valid portal sign. | Break succeeds. Revoke the grant after the check. | Manual |
| V-018 | LuckPerms deny blocks portal sign use | Deny `DefaultPlayer` `signport.sign.use`, then right-click a valid portal sign. | Teleport is blocked and the player sees `You do not have permissions to use port signs.` Remove the deny after the check. | Manual |
| V-019 | Invalid dimension line does not crash | Create a portal sign with line 2 `[sp]`, line 3 `missing_anchor`, and line 4 `not a dimension`. Right-click it. | Server does not crash. Sign falls back to normal interaction and the marker turns red after validation. | Manual |
| V-020 | Front sign text is preferred | Put an invalid portal on the back and a valid portal on the front. Right-click from the front. | Player teleports using the front-side target. | Manual |
| V-021 | Back sign text fallback works | Put a valid portal on the back and non-portal text on the front. Right-click from the front. | Player teleports using the back-side target. | Manual |
| V-022 | `/signport tp` default is everyone | As `DefaultPlayer` with no grants, run `/signport tp spawn`. | Player teleports to `spawn`. | Manual |
| V-023 | LuckPerms deny blocks `/signport tp` | Deny `DefaultPlayer` `signport.teleport.command`, then run `/signport tp spawn`. | Command is unavailable or rejected by command permission checks. Remove the deny after the check. | Manual |

Equivalent `/sp` aliases should be spot-checked for command paths, for example `/sp anchor list` and `/sp tp spawn`.

## Suggested LuckPerms Commands

Adapt these commands to the test player or group names used on the server:

```text
lp user DefaultPlayer permission set signport.anchor.create true
lp user DefaultPlayer permission unset signport.anchor.create
lp user DefaultPlayer permission set signport.anchor.list true
lp user DefaultPlayer permission unset signport.anchor.list
lp user DefaultPlayer permission set signport.anchor.delete true
lp user DefaultPlayer permission unset signport.anchor.delete
lp user DefaultPlayer permission set signport.sign.create true
lp user DefaultPlayer permission unset signport.sign.create
lp user DefaultPlayer permission set signport.sign.edit true
lp user DefaultPlayer permission unset signport.sign.edit
lp user DefaultPlayer permission set signport.sign.break true
lp user DefaultPlayer permission unset signport.sign.break
lp user DefaultPlayer permission set signport.sign.use false
lp user DefaultPlayer permission unset signport.sign.use
lp user DefaultPlayer permission set signport.teleport.command false
lp user DefaultPlayer permission unset signport.teleport.command
```

## Executable Coverage

The repository currently has unit coverage for portal marker parsing and dimension ID parsing in `PortSignFormatTest`. The full permission-provider behavior, sign editing hooks, sign breaking hooks, front/back interaction, and player teleport behavior require an in-game server validation pass because they depend on Minecraft server events, mixins, player state, and LuckPerms runtime integration.
