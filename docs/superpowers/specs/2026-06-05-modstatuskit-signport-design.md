# SignPort ModStatusKit Integration Design

## Goal

Add ModStatusKit (MSK) to SignPort as an embedded, relocated internal library so the optional client can show passive client/server status without exposing MSK as a public dependency.

## Scope

- Embed the current dependency-free MSK Java helpers from `cloud.explosive.modstatuskit` under `tech.endorsed.signport.internal.modstatus`.
- Keep Fabric networking in SignPort-owned code. MSK provides payload/status/display helpers only.
- Add one SignPort server-version status payload plus a tiny client request payload so status delivery does not depend on anchor sync. Use the server join push as the primary CBA-aligned signal and keep the client request as a retry fallback.
- Reset status on disconnect and represent unknown, disconnected, and server-not-detected states as informational.
- Add status display to the optional client settings surface.
- Document the status behavior and add a public changelog entry.

Out of scope:

- Publishing, releasing, tagging, pushing, or merging.
- Editing ModStatusKit or sibling repositories.
- Treating version mismatch as a gameplay incompatibility unless SignPort later introduces a real protocol break.
- Adding ModMenu as a required dependency.

## Architecture

MSK core classes live in `src/main/java/tech/endorsed/signport/internal/modstatus/`. They remain plain Java and do not import Fabric or Minecraft classes. SignPort-specific integration code builds a `ModStatusConfig` with SignPort metadata, wraps MSK byte payloads in Fabric `CustomPacketPayload`, and owns receiver registration, capability checks, lifecycle hooks, and UI rendering.

The server status payload uses `signport:server_version`, matching the Carry Baby Animals reference pattern. The server attempts a CBA-style status push from `ServerPlayConnectionEvents.JOIN` when the client advertises the status payload channel. SignPort also owns a dedicated `signport:server_version_request` client-to-server request payload as a fallback so delivery does not depend on anchor sync. The client repeats the status request at a small interval until a real status payload arrives, even if the UI has already moved to the informational `Server not detected` state. Anchor sync remains separate; receiving or requesting anchor data must not be required for the MSK status payload to arrive. The client registers the same payload type and updates `ModStatusClientState` when a payload is received.

The client starts disconnected, moves to unknown on join, moves to connected when a payload arrives, and moves to server-not-detected after a short tick grace period if the server never advertises the channel. Disconnect returns the state to disconnected. This keeps vanilla servers and servers without the new status payload non-alarming.

## Build Metadata

The baseline version comes from `fabric.mod.json` via Fabric Loader metadata. Build metadata is optional. If SignPort adds a generated resource, it should be packaged inside the jar and used only for diagnostics. It must not require Git access at runtime.

## UI

The client settings screen gets a compact top-right status indicator aligned with the MSK 0.1.8 and Carry Baby Animals reference UI. It displays:

- Compact colored square indicator in the screen chrome.
- Hover tooltip with status label, passive help text from MSK, client version/build, and server version/build or `Unknown`.

Tone maps through `ModStatusDisplay.tone()` using the reference green, teal, orange, red, and gray colors in the config screen only. No gameplay HUD warning is added.

## Tests And Verification

Focused tests cover:

- MSK payload encode/decode and client-state transitions.
- SignPort status payload codec behavior.
- Client lifecycle decision helper for unknown/server-not-detected transitions.
- Metadata/entrypoint safety where touched.

Final verification:

- Run focused tests while iterating.
- Run the quiet full build pattern from `AGENTS.md`.
- Inspect the built jar with `jar tf` to prove internal relocated MSK classes are present and `cloud/explosive/modstatuskit` classes are absent.

## Risks

- Fabric networking API names differ from MSK's generic embedded example. The implementation must follow SignPort's existing MC 26.1.2 Mojang/Fabric API style.
- The client settings UI should remain client-only and must not make ModMenu required.
