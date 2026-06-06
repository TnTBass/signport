# ModStatusKit SignPort Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Embed relocated ModStatusKit in SignPort and expose passive client/server status through existing Fabric networking and optional config UI.

**Architecture:** Plain Java MSK helpers are copied under `tech.endorsed.signport.internal.modstatus`. SignPort owns a dedicated `server_version` Fabric payload, a CBA-style server join push, an independent `server_version_request` client fallback payload, client lifecycle state, and a compact config-screen status display.

**Tech Stack:** Java 25, Fabric Loader/API for Minecraft 26.1.2, Mojang mappings, JUnit 6, vanilla client settings screen, optional ModMenu entrypoint.

---

## File Structure

- Create `src/main/java/tech/endorsed/signport/internal/modstatus/*.java`: relocated MSK helper classes.
- Create `src/main/java/tech/endorsed/signport/status/SignPortStatus.java`: SignPort-owned MSK config and lifecycle helpers.
- Create `src/main/java/tech/endorsed/signport/network/StatusPayloads.java`: Fabric status payload type/codec registration.
- Create `src/main/java/tech/endorsed/signport/network/SignPortStatusNetworking.java`: independent CBA-style status networking registration, server join push, server request receiver, and client-request decision helper.
- Modify `src/main/java/tech/endorsed/signport/network/AnchorSyncServer.java`: keep status delivery out of anchor sync state.
- Modify `src/client/java/tech/endorsed/signport/client/SignPortClient.java`: register client receiver, update status lifecycle, and reset on disconnect.
- Modify `src/client/java/tech/endorsed/signport/client/config/SignPortConfigScreen.java`: render passive status indicator in config UI.
- Modify `README.md` and `CHANGELOG.md`: document player/admin-visible status behavior.
- Add tests under `src/test/java/tech/endorsed/signport/internal/modstatus/`, `src/test/java/tech/endorsed/signport/status/`, and `src/test/java/tech/endorsed/signport/network/`.

---

### Task 1: Embed MSK Core

**Files:**
- Create: `src/main/java/tech/endorsed/signport/internal/modstatus/ModStatusClientState.java`
- Create: `src/main/java/tech/endorsed/signport/internal/modstatus/ModStatusConfig.java`
- Create: `src/main/java/tech/endorsed/signport/internal/modstatus/ModStatusDisplay.java`
- Create: `src/main/java/tech/endorsed/signport/internal/modstatus/ModStatusKit.java`
- Create: `src/main/java/tech/endorsed/signport/internal/modstatus/ModStatusMessages.java`
- Create: `src/main/java/tech/endorsed/signport/internal/modstatus/ModStatusServerStatus.java`
- Create: `src/main/java/tech/endorsed/signport/internal/modstatus/ModStatusSnapshot.java`
- Create: `src/main/java/tech/endorsed/signport/internal/modstatus/ModStatusStrings.java`
- Create: `src/main/java/tech/endorsed/signport/internal/modstatus/ModStatusVersion.java`
- Create: `src/main/java/tech/endorsed/signport/internal/modstatus/ModStatusVersionPayload.java`
- Create: `src/main/java/tech/endorsed/signport/internal/modstatus/StatusTone.java`
- Create: `src/main/java/tech/endorsed/signport/internal/modstatus/VersionMismatchSeverity.java`
- Create: `src/main/java/tech/endorsed/signport/internal/modstatus/VersionStatus.java`
- Test: `src/test/java/tech/endorsed/signport/internal/modstatus/ModStatusKitTest.java`

- [ ] **Step 1: Write relocated MSK tests**

Create tests for payload round-trip, disconnected default state, connected matching version, and build-different teal tone.

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat --quiet test --tests tech.endorsed.signport.internal.modstatus.ModStatusKitTest`
Expected: FAIL because relocated classes do not exist.

- [ ] **Step 3: Copy current MSK core classes**

Copy the current dependency-free files from local ModStatusKit and change only the package declaration to `tech.endorsed.signport.internal.modstatus`. Keep the embedded layer plain Java. If an upstream helper imports Fabric or Minecraft types, move that binding into SignPort-owned code instead of copying the import into `internal.modstatus`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat --quiet test --tests tech.endorsed.signport.internal.modstatus.ModStatusKitTest`
Expected: PASS.

- [ ] **Step 5: Verify no external MSK dependency was added**

Run: `Select-String -Path build.gradle -Pattern 'modstatuskit|cloud.explosive' -CaseSensitive:$false`
Expected: no output. The integration is embedded/relocated source, not a Gradle dependency.

---

### Task 2: Add SignPort Status Config And Payload

**Files:**
- Create: `src/main/java/tech/endorsed/signport/status/SignPortStatus.java`
- Create: `src/main/java/tech/endorsed/signport/network/StatusPayloads.java`
- Test: `src/test/java/tech/endorsed/signport/status/SignPortStatusTest.java`
- Test: `src/test/java/tech/endorsed/signport/network/StatusPayloadsTest.java`

- [ ] **Step 1: Write tests**

Cover status config values, passive messages, server-not-detected transition helper, status payload codec byte round-trip, empty server-version request codec behavior, and the client request decision helper.

- [ ] **Step 2: Run tests to verify failure**

Run: `.\gradlew.bat --quiet test --tests tech.endorsed.signport.status.SignPortStatusTest --tests tech.endorsed.signport.network.StatusPayloadsTest`
Expected: FAIL because classes do not exist.

- [ ] **Step 3: Implement status config and payload**

Use `SignPort.MOD_ID`, display name `SignPort`, payload channel `signport:server_version`, update URL `https://github.com/TnTBass/signport`, and `VersionMismatchSeverity.WARN`.

- [ ] **Step 4: Run tests to verify pass**

Run: `.\gradlew.bat --quiet test --tests tech.endorsed.signport.status.SignPortStatusTest --tests tech.endorsed.signport.network.StatusPayloadsTest`
Expected: PASS.

---

### Task 3: Wire Server And Client Lifecycle

**Files:**
- Modify: `src/main/java/tech/endorsed/signport/network/AnchorSyncServer.java`
- Modify: `src/client/java/tech/endorsed/signport/client/SignPortClient.java`
- Test: update `src/test/java/tech/endorsed/signport/network/StatusPayloadsTest.java`
- Test: update `src/test/java/tech/endorsed/signport/status/SignPortStatusTest.java`

- [ ] **Step 1: Add lifecycle tests**

Add pure helper tests for join/receive/missing-status timeout behavior. Keep Minecraft/Fabric runtime objects out of unit tests. The timeout helper must accept a deterministic tick counter, elapsed tick count, or equivalent monotonic integer so tests do not sleep and do not need a running client.

- [ ] **Step 2: Run tests to verify failure**

Run the two focused test classes. Expected: FAIL for missing helper behavior.

- [ ] **Step 3: Register and send status**

Register `StatusPayloads.registerClientbound()` and `StatusPayloads.registerServerbound()` from the dedicated status networking helper and the client init path. The server sends `signport:server_version` from `ServerPlayConnectionEvents.JOIN` when the client advertises the payload channel, matching the CBA reference. The client also sends `signport:server_version_request` when `ClientPlayNetworking.canSend(StatusPayloads.REQUEST_TYPE)` becomes true and no status has been received, then retries at a small interval until a real status payload arrives; the server replies with the normal `signport:server_version` payload. Do not send or retry MSK status from `AnchorSyncServer` or `AnchorSyncPayloads.READY_TYPE`. On client join, call `SignPortStatus.onClientJoin()`. On receive, decode with MSK and update the client state. On disconnect/client stopping, reset to disconnected.

- [ ] **Step 4: Run focused tests**

Run the two focused test classes. Expected: PASS.

---

### Task 4: Add Optional Config UI Status Indicator

**Files:**
- Modify: `src/client/java/tech/endorsed/signport/client/config/SignPortConfigScreen.java`
- Test: add focused pure tests if helper formatting is split out to `SignPortStatus`.

- [ ] **Step 1: Add formatting tests**

Test client/server version formatting with and without build metadata.

- [ ] **Step 2: Run tests to verify failure**

Run: `.\gradlew.bat --quiet test --tests tech.endorsed.signport.status.SignPortStatusTest`

- [ ] **Step 3: Add top-right status indicator to config screen**

Display a compact square status indicator with hover details for status label/help text plus client/server version. Keep it passive; do not block gameplay or imply incompatibility.

- [ ] **Step 4: Run focused tests**

Run the status test class. Expected: PASS.

---

### Task 5: Docs, Changelog, And Verification

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Update docs**

Document that SignPort-equipped clients show passive server status in the optional settings screen. State that vanilla clients and servers without the status payloads continue to work.

- [ ] **Step 2: Update public changelog**

Add one `## Unreleased` bullet for the player/admin-visible status display.

- [ ] **Step 3: Run narrow tests**

Run focused test classes added or changed in this plan.

- [ ] **Step 4: Run full quiet build**

Run:
```powershell
.\gradlew.bat --quiet build > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED|cannot find symbol|incompatible types|mixin apply failed' | Select-Object -First 80
```
Expected: no matching error output.

- [ ] **Step 5: Inspect jar**

Run:
```powershell
jar tf build\libs\signport-2.2.2+mc26.1.2.jar | Select-String -Pattern 'tech/endorsed/signport/internal/modstatus|cloud/explosive/modstatuskit'
```
Expected: relocated `tech/endorsed/signport/internal/modstatus` classes present; no `cloud/explosive/modstatuskit` classes.

Use a version-independent command if the jar name changes before verification:

```powershell
Get-Item build\libs\signport-*.jar | ForEach-Object { jar tf $_.FullName } | Select-String -Pattern 'tech/endorsed/signport/internal/modstatus|cloud/explosive/modstatuskit'
```
