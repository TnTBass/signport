# SignPort ModStatusKit Integration Design

## Goal

Add ModStatusKit (MSK) to SignPort as an embedded, relocated internal library so the optional client can show passive client/server status without exposing MSK as a public dependency.

## Scope

- Embed the current dependency-free MSK Java helpers from `cloud.explosive.modstatuskit` under `tech.endorsed.signport.internal.modstatus`.
- Keep Fabric networking in SignPort-owned code. MSK provides payload/status/display helpers only.
- Add one SignPort status payload carrying server version/build status to clients that advertise support for the status channel.
- Reset status on disconnect and represent unknown, disconnected, and server-not-detected states as informational.
- Add status display to the existing optional Cloth Config/ModMenu config surface.
- Document the status behavior and add a public changelog entry.

Out of scope:

- Publishing, releasing, tagging, pushing, or merging.
- Editing ModStatusKit or sibling repositories.
- Treating version mismatch as a gameplay incompatibility unless SignPort later introduces a real protocol break.
- Adding ModMenu or Cloth Config as required dependencies.

## Architecture

MSK core classes live in `src/main/java/tech/endorsed/signport/internal/modstatus/`. They remain plain Java and do not import Fabric or Minecraft classes. SignPort-specific integration code builds a `ModStatusConfig` with SignPort metadata, wraps MSK byte payloads in Fabric `CustomPacketPayload`, and owns receiver registration, capability checks, lifecycle hooks, and UI rendering.

The status payload uses `signport:status_version`. The server registers it as clientbound and sends on player join when `ServerPlayNetworking.canSend(player, TYPE)` is true. The client registers the same payload type and updates `ModStatusClientState` when a payload is received.

The client starts disconnected, moves to unknown on join, moves to connected when a payload arrives, and moves to server-not-detected after a short tick grace period if the server never advertises the channel. Disconnect returns the state to disconnected. This keeps vanilla servers and servers without the new status payload non-alarming.

## Build Metadata

The baseline version comes from `fabric.mod.json` via Fabric Loader metadata. Build metadata is optional. If SignPort adds a generated resource, it should be packaged inside the jar and used only for diagnostics. It must not require Git access at runtime.

## UI

The existing Cloth Config bridge gets a compact status category or row. It displays:

- Status label and passive help text from MSK.
- Client version/build.
- Server version/build or `Unknown`.

Tone maps to restrained colors in the config screen only. No gameplay HUD warning is added.

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
- Client config UI imports optional Cloth Config APIs, so status UI code that imports Cloth types must remain in the existing optional bridge path.
