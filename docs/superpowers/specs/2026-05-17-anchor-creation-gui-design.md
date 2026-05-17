# SignPort Anchor Creation GUI Design

Date: 2026-05-17

## Summary

Add a client-mod-only anchor creation flow to the existing SignPort anchor browser. Vanilla players continue to create anchors with `/sp anchor create`; players with the optional SignPort client mod can press the existing anchor browser keybind, open a focused create modal, and create an anchor at their current server-side position.

The server remains authoritative for permissions, position, dimension, validation, storage, BlueMap updates, and anchor sync.

## Goals

- Let players create anchors from the optional client GUI instead of typing the create command.
- Reuse the existing anchor browser keybind as the entry point.
- Create anchors at the player current exact position and dimension, matching `/sp anchor create <name>`.
- Support an optional group at creation time.
- Give immediate visual validation that matches SignPort sign feedback: green for acceptable, red for invalid, and orange for cross-dimension name ambiguity.
- Preserve vanilla-client compatibility with no GUI fallback required beyond existing commands.

## Non-Goals

- No vanilla chat-menu or server-only GUI replacement.
- No position editing, dimension picking, or remote anchor creation.
- No anchor rename flow.
- No local optimistic anchor creation before the server accepts the request.
- No new required third-party client UI library.

## Entry Point

The existing anchor browser keybind continues to open the anchor browser. The browser adds a `Create` button near its existing search, sort, and navigation controls.

The `Create` button is available only when all of these are true:

- The connected server supports the SignPort sync protocol.
- The client has received a permission snapshot.
- `PermissionSnapshot.canCreateAnchor()` is true.

If any condition is false, the GUI does not offer creation. Vanilla players and players without permission continue to use the existing command surface when appropriate.

## Create Modal

Clicking `Create` opens a small modal over the anchor browser. The browser remains in the background; the modal owns focus until the player creates or cancels.

Fields:

- `Anchor name`
- `Group`, optional
- `Current location`, read-only live preview

Actions:

- `Create`
- `Cancel`

The modal uses vanilla Minecraft widgets and the existing SignPort client style rather than introducing a new visual system.

## Anchor Name Validation

The anchor name field is the primary status indicator.

- Green: name is non-blank, within length, valid, and unused in the current dimension.
- Orange: name is valid and unused in the current dimension, but the same name exists in at least one other synced dimension.
- Red: name is blank, too long, invalid, or already exists in the current dimension.

The `Create` button is disabled when the name is red. It remains enabled when the name is orange.

When orange, the modal shows this warning:

```text
Name exists in another dimension. Signs may need a dimension line.
```

The client uses the synced anchor cache for immediate duplicate feedback. The server still performs authoritative validation because the cache may be stale.

The max length for GUI-created anchor names should be slightly shorter than the practical one-line sign limit, by one or two characters. The implementation plan should verify the best constant against the current sign editing behavior and existing SignPort formatting tests before choosing the exact value.

## Group Suggestions

The group field remains optional free text.

As the player types, the modal suggests existing groups from the synced anchor cache. Suggestions are normalized in the same spirit as command-created groups so players do not accidentally create near-duplicates such as `spawn`, `Spawn`, and `spawns`. Selecting a suggestion fills the group field.

The server still normalizes and stores the submitted group using the same behavior as the command path.

## Location Preview

The modal shows a live read-only preview of the player current dimension and integer block position.

The preview is informational only. The client does not send a position or dimension in the create request. On submit, the server derives the position and dimension from the sending player. If the player moves while the modal is open, the anchor is created at the server-side position when they click `Create`.

## Networking

Add a dedicated serverbound payload, conceptually:

```text
CreateAnchorRequest(name, group)
```

The payload contains only the player-entered fields. It does not include position, dimension, permission state, or created timestamp.

The server registers the payload alongside the existing serverbound ready handshake. On receipt, it runs on the server thread, gets the sending `ServerPlayer`, checks `SignPortPermissions.canCreateAnchor`, derives the player `BlockPos` and `ResourceKey<Level>`, and routes creation through shared anchor creation logic.

## Server Behavior

The command and GUI request should share the same creation behavior:

- Permission checks use `signport.anchor.create`.
- Anchor name collision remains scoped to the current dimension.
- Duplicate exact position in the current dimension is rejected, matching current command behavior.
- Group normalization matches existing command behavior.
- Successful creation marks anchor state dirty.
- BlueMap integration receives the same create notification.
- Anchor sync broadcasts the same create delta.
- The player receives clear success or failure feedback.

If the current command implementation keeps creation logic embedded in `AnchorCommand`, implementation should extract a small shared server-side helper rather than duplicating validation in networking code.

## Response And Error Handling

The modal enters a pending state after submit. During pending state, the form should prevent double-submit.

On success:

- The modal closes.
- The browser remains open.
- The anchor appears via the existing sync delta, not by local optimistic insertion.

On rejection:

- The modal stays open.
- The anchor name/status area turns red.
- A short server-derived error message is shown.
- The player can edit and retry.

Likely rejection cases:

- Permission changed or was not actually granted.
- Name already exists in the current dimension.
- Another anchor already exists at the current position.
- Name is invalid or over length.
- Client cache was stale.

## Testing

Focused tests should cover the shared server-side validation/helper where possible:

- Creates an anchor using the current player position and dimension.
- Rejects duplicate names in the same dimension.
- Allows same name in a different dimension, with client warning behavior covered separately if practical.
- Rejects duplicate position in the same dimension.
- Normalizes optional group consistently with command creation.
- Requires anchor create permission.

Client-side behavior should be covered where the current test setup allows:

- Name validation color state: green, orange, red.
- `Create` disabled only for red states.
- Group suggestions come from synced groups.
- Create request payload omits position and dimension.

Run the full quiet build before implementation completion.

## Open Implementation Notes

- Verify the exact GUI anchor-name max length against sign editing behavior and existing SignPort formatting tests.
- Decide whether server rejection feedback needs a new response payload or can be represented through existing chat/system messages plus client-side timeout handling. A direct response payload is likely cleaner because the modal should wait for success before closing.
- Keep all visual companion artifacts under `.superpowers/`, which is local scratch and ignored by git.
