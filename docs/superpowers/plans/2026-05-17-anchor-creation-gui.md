# Anchor Creation GUI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a client-mod-only GUI flow for creating SignPort anchors from the existing anchor browser.

**Architecture:** Keep the server authoritative. The client browser opens a modal and sends `CreateAnchorRequest(name, group)`; the server derives player position/dimension, validates through a shared creation helper, sends `CreateAnchorResponse(success, errorMessage)`, and relies on the existing anchor delta sync for list updates.

**Tech Stack:** Java 25, Fabric API networking, Minecraft 26.1.2 Mojang mappings, JUnit 5, existing SignPort client source set.

---

## File Structure

- Modify `CHANGELOG.md`
  - Add an Unreleased user-facing bullet for GUI anchor creation.
- Create `src/main/java/tech/endorsed/signport/world/AnchorCreation.java`
  - Pure helper for validation and state mutation.
  - Owns `MAX_ANCHOR_NAME_LENGTH`, `normalizeGroup`, `create(...)`, and result/error types.
  - Does not send chat, BlueMap updates, or network packets.
- Modify `src/main/java/tech/endorsed/signport/command/AnchorCommand.java`
  - Replace embedded create validation/mutation with `AnchorCreation.create(...)`.
  - Keep command-specific permission, chat feedback, BlueMap notification, and anchor sync.
- Modify `src/main/java/tech/endorsed/signport/world/AnchorState.java`
  - Use `AnchorCreation.normalizeGroup(...)` so command, state, client tests, and GUI share group-string behavior.
- Modify `src/main/java/tech/endorsed/signport/network/AnchorSyncPayloads.java`
  - Add `CreateAnchorRequest` serverbound payload and codec.
  - Add `CreateAnchorResponse` clientbound payload and codec.
  - Register both with existing idempotent helpers.
- Modify `src/main/java/tech/endorsed/signport/network/AnchorSyncServer.java`
  - Register the create request receiver.
  - Derive sender position/dimension server-side.
  - Reuse `AnchorCreation`.
  - Send `CreateAnchorResponse` only to the requesting player.
  - On success, notify BlueMap and existing anchor sync.
- Modify `src/client/java/tech/endorsed/signport/client/SignPortClient.java`
  - Register the create response receiver.
  - Expose a small callback path to the open anchor browser.
- Modify `src/client/java/tech/endorsed/signport/client/gui/AnchorBrowserScreen.java`
  - Add permission-gated `Create` button.
  - Add modal state, fields, validation colors, group suggestions, live location preview, pending state, and response handling.
- Modify `src/main/resources/assets/signport/lang/en_us.json`
  - Add user-facing labels and warnings if implementation chooses translation keys over literals.
- Test `src/test/java/tech/endorsed/signport/world/AnchorCreationTest.java`
  - Cover pure creation rules.
- Modify `src/test/java/tech/endorsed/signport/world/AnchorStateTest.java`
  - Cover group normalization consistency if not fully covered by `AnchorCreationTest`.
- Modify `src/test/java/tech/endorsed/signport/network/AnchorSyncPayloadsTest.java`
  - Cover request/response payload behavior and the name-length constant.
- Modify or add client-safe tests only if current test setup can instantiate the relevant client helper without Minecraft runtime.

---

### Task 1: Add Changelog Entry

**Files:**
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Add an Unreleased bullet**

Add this bullet under `## Unreleased`:

```markdown
- Added a client-side anchor creation form to the anchor browser for players with anchor creation permission.
```

- [ ] **Step 2: Verify the changelog diff**

Run:

```powershell
git diff -- CHANGELOG.md
```

Expected: one Unreleased bullet added, no version sections changed.

- [ ] **Step 3: Commit**

Run:

```powershell
git add CHANGELOG.md
git commit -m "Document anchor creation GUI"
```

Expected: commit succeeds.

---

### Task 2: Extract Pure Anchor Creation Rules

**Files:**
- Create: `src/main/java/tech/endorsed/signport/world/AnchorCreation.java`
- Modify: `src/main/java/tech/endorsed/signport/world/AnchorState.java`
- Modify: `src/main/java/tech/endorsed/signport/command/AnchorCommand.java`
- Test: `src/test/java/tech/endorsed/signport/world/AnchorCreationTest.java`

- [ ] **Step 1: Write failing helper tests**

Create `src/test/java/tech/endorsed/signport/world/AnchorCreationTest.java`:

```java
package tech.endorsed.signport.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorCreationTest {
    private static final ResourceKey<Level> OVERWORLD = Level.OVERWORLD;
    private static final ResourceKey<Level> NETHER =
            ResourceKey.create(Registries.DIMENSION, Identifier.tryParse("minecraft:the_nether"));

    @Test
    void createsAnchorAtSuppliedPositionAndDimension() {
        AnchorState state = new AnchorState();

        AnchorCreation.Result result = AnchorCreation.create(
                state, "spawn", new BlockPos(1, 64, 2), OVERWORLD, "bases");

        assertTrue(result.success());
        Anchor anchor = state.findAnchor("spawn", OVERWORLD).orElseThrow();
        assertEquals(new BlockPos(1, 64, 2), anchor.pos);
        assertEquals(OVERWORLD, anchor.dimension);
        assertEquals("bases", anchor.group);
        assertEquals(AnchorCreation.Error.NONE, result.error());
    }

    @Test
    void rejectsBlankName() {
        AnchorCreation.Result result = AnchorCreation.create(
                new AnchorState(), " ", new BlockPos(1, 64, 2), OVERWORLD, "");

        assertFalse(result.success());
        assertEquals(AnchorCreation.Error.INVALID_NAME, result.error());
    }

    @Test
    void rejectsNamesLongerThanGuiSignLineLimit() {
        String tooLong = "a".repeat(AnchorCreation.MAX_ANCHOR_NAME_LENGTH + 1);

        AnchorCreation.Result result = AnchorCreation.create(
                new AnchorState(), tooLong, new BlockPos(1, 64, 2), OVERWORLD, "");

        assertFalse(result.success());
        assertEquals(AnchorCreation.Error.INVALID_NAME, result.error());
    }

    @Test
    void rejectsDuplicateNameInSameDimension() {
        AnchorState state = new AnchorState();
        state.addAnchor(new Anchor("spawn", new BlockPos(1, 64, 2), OVERWORLD));

        AnchorCreation.Result result = AnchorCreation.create(
                state, "spawn", new BlockPos(3, 64, 4), OVERWORLD, "");

        assertFalse(result.success());
        assertEquals(AnchorCreation.Error.NAME_CLASH, result.error());
    }

    @Test
    void allowsSameNameInDifferentDimension() {
        AnchorState state = new AnchorState();
        state.addAnchor(new Anchor("spawn", new BlockPos(1, 64, 2), OVERWORLD));

        AnchorCreation.Result result = AnchorCreation.create(
                state, "spawn", new BlockPos(3, 64, 4), NETHER, "");

        assertTrue(result.success());
        assertTrue(state.findAnchor("spawn", NETHER).isPresent());
    }

    @Test
    void rejectsDuplicatePositionInSameDimension() {
        AnchorState state = new AnchorState();
        state.addAnchor(new Anchor("spawn", new BlockPos(1, 64, 2), OVERWORLD));

        AnchorCreation.Result result = AnchorCreation.create(
                state, "shop", new BlockPos(1, 64, 2), OVERWORLD, "");

        assertFalse(result.success());
        assertEquals(AnchorCreation.Error.POSITION_CLASH, result.error());
    }

    @Test
    void groupStringsMatchExistingServerBehavior() {
        assertEquals("", AnchorCreation.normalizeGroup(null));
        assertEquals("", AnchorCreation.normalizeGroup("-"));
        assertEquals("Spawn", AnchorCreation.normalizeGroup("Spawn"));
        assertEquals("spawns", AnchorCreation.normalizeGroup("spawns"));
    }
}
```

- [ ] **Step 2: Run failing helper tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests tech.endorsed.signport.world.AnchorCreationTest > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED|cannot find symbol|incompatible types' | Select-Object -First 80
```

Expected: failure because `AnchorCreation` does not exist.

- [ ] **Step 3: Implement `AnchorCreation`**

Create `src/main/java/tech/endorsed/signport/world/AnchorCreation.java`:

```java
package tech.endorsed.signport.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class AnchorCreation {
    public static final int MAX_ANCHOR_NAME_LENGTH = 14;
    public static final String CLEAR_GROUP_SENTINEL = "-";

    private AnchorCreation() {
    }

    public static Result create(
            AnchorState state,
            String name,
            BlockPos pos,
            ResourceKey<Level> dimension,
            String group
    ) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty() || normalizedName.length() > MAX_ANCHOR_NAME_LENGTH) {
            return Result.failure(Error.INVALID_NAME);
        }
        if (state.findAnchor(normalizedName, dimension).isPresent()) {
            return Result.failure(Error.NAME_CLASH);
        }
        for (Anchor anchor : state.getAnchorsForDimension(dimension)) {
            if (anchor.pos.equals(pos)) {
                return Result.failure(Error.POSITION_CLASH);
            }
        }

        Anchor anchor = new Anchor(normalizedName, pos, dimension, normalizeGroup(group));
        state.addAnchor(anchor);
        return Result.success(anchor);
    }

    public static String normalizeGroup(String group) {
        if (group == null || group.equals(CLEAR_GROUP_SENTINEL)) return "";
        return group;
    }

    public enum Error {
        NONE(""),
        INVALID_NAME("Invalid anchor name"),
        NAME_CLASH("An anchor with that name already exists in this dimension"),
        POSITION_CLASH("An anchor already exists at this position");

        private final String message;

        Error(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    public record Result(boolean success, Anchor anchor, Error error) {
        public static Result success(Anchor anchor) {
            return new Result(true, anchor, Error.NONE);
        }

        public static Result failure(Error error) {
            return new Result(false, null, error);
        }

        public String errorMessage() {
            return error.message();
        }
    }
}
```

- [ ] **Step 4: Reuse `AnchorCreation.normalizeGroup` in `AnchorState`**

In `src/main/java/tech/endorsed/signport/world/AnchorState.java`, replace the private `normalizeGroup` method body with:

```java
private static String normalizeGroup(String group) {
    return AnchorCreation.normalizeGroup(group);
}
```

- [ ] **Step 5: Reuse `AnchorCreation` in `AnchorCommand.createAnchor`**

In `src/main/java/tech/endorsed/signport/command/AnchorCommand.java`, add:

```java
import tech.endorsed.signport.world.AnchorCreation;
```

Then replace the validation/mutation block inside `createAnchor` after `var aPos = ...` with:

```java
AnchorCreation.Result result = AnchorCreation.create(anchorState, name, aPos, dim, group);
if (!result.success()) {
    if (result.error() == AnchorCreation.Error.NAME_CLASH) throw NAME_CLASH_EXCEPTION.create();
    throw CREATE_FAILED_EXCEPTION.create();
}

Anchor anchor = result.anchor();
String normalizedGroup = anchor.group;
BlueMapIntegration.anchorCreated(source.getServer(), anchor);
AnchorSyncServer.anchorCreated(source.getServer(), anchor);
```

Leave the existing success chat message logic in place, using `normalizedGroup`.

- [ ] **Step 6: Run helper tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests tech.endorsed.signport.world.AnchorCreationTest > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED|cannot find symbol|incompatible types' | Select-Object -First 80
```

Expected: no output from `Select-String`.

- [ ] **Step 7: Commit**

Run:

```powershell
git add src/main/java/tech/endorsed/signport/world/AnchorCreation.java src/main/java/tech/endorsed/signport/world/AnchorState.java src/main/java/tech/endorsed/signport/command/AnchorCommand.java src/test/java/tech/endorsed/signport/world/AnchorCreationTest.java
git commit -m "Extract anchor creation rules"
```

Expected: commit succeeds.

---

### Task 3: Add Create Request And Response Payloads

**Files:**
- Modify: `src/main/java/tech/endorsed/signport/network/AnchorSyncPayloads.java`
- Test: `src/test/java/tech/endorsed/signport/network/AnchorSyncPayloadsTest.java`

- [ ] **Step 1: Write failing payload tests**

Append these tests to `AnchorSyncPayloadsTest`:

```java
    @Test
    void createRequestAcceptsNamesAtSharedMaxLength() {
        String max = "a".repeat(tech.endorsed.signport.world.AnchorCreation.MAX_ANCHOR_NAME_LENGTH);
        AnchorSyncPayloads.CreateAnchorRequest request = new AnchorSyncPayloads.CreateAnchorRequest(max, "Spawn");

        assertTrue(request.name().length() <= tech.endorsed.signport.world.AnchorCreation.MAX_ANCHOR_NAME_LENGTH);
        assertTrue(request.type().id().toString().contains("anchor_create_request"));
    }

    @Test
    void createResponseCarriesSuccessAndFailureText() {
        AnchorSyncPayloads.CreateAnchorResponse success = AnchorSyncPayloads.CreateAnchorResponse.success();
        AnchorSyncPayloads.CreateAnchorResponse failure = AnchorSyncPayloads.CreateAnchorResponse.failure("Duplicate name");

        assertTrue(success.success());
        assertTrue(success.errorMessage().isEmpty());
        assertFalse(failure.success());
        assertTrue(failure.errorMessage().contains("Duplicate"));
    }
```

- [ ] **Step 2: Run failing payload tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests tech.endorsed.signport.network.AnchorSyncPayloadsTest > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED|cannot find symbol|incompatible types' | Select-Object -First 80
```

Expected: compile failure because `CreateAnchorRequest` and `CreateAnchorResponse` do not exist.

- [ ] **Step 3: Add payload types and codecs**

In `AnchorSyncPayloads`, add type constants:

```java
public static final CustomPacketPayload.Type<CreateAnchorRequest> CREATE_ANCHOR_REQUEST_TYPE =
        new CustomPacketPayload.Type<>(id("anchor_create_request"));
public static final CustomPacketPayload.Type<CreateAnchorResponse> CREATE_ANCHOR_RESPONSE_TYPE =
        new CustomPacketPayload.Type<>(id("anchor_create_response"));
```

Add codecs:

```java
public static final StreamCodec<RegistryFriendlyByteBuf, CreateAnchorRequest> CREATE_ANCHOR_REQUEST_CODEC = StreamCodec.of(
        AnchorSyncPayloads::writeCreateAnchorRequest,
        AnchorSyncPayloads::readCreateAnchorRequest);
public static final StreamCodec<RegistryFriendlyByteBuf, CreateAnchorResponse> CREATE_ANCHOR_RESPONSE_CODEC = StreamCodec.of(
        AnchorSyncPayloads::writeCreateAnchorResponse,
        AnchorSyncPayloads::readCreateAnchorResponse);
```

Add records:

```java
public record CreateAnchorRequest(String name, String group) implements CustomPacketPayload {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return CREATE_ANCHOR_REQUEST_TYPE;
    }
}

public record CreateAnchorResponse(boolean success, String errorMessage) implements CustomPacketPayload {
    public static CreateAnchorResponse success() {
        return new CreateAnchorResponse(true, "");
    }

    public static CreateAnchorResponse failure(String errorMessage) {
        return new CreateAnchorResponse(false, errorMessage == null ? "" : errorMessage);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return CREATE_ANCHOR_RESPONSE_TYPE;
    }
}
```

Add codec helpers:

```java
private static void writeCreateAnchorRequest(RegistryFriendlyByteBuf buf, CreateAnchorRequest payload) {
    buf.writeUtf(payload.name(), tech.endorsed.signport.world.AnchorCreation.MAX_ANCHOR_NAME_LENGTH);
    buf.writeUtf(payload.group() == null ? "" : payload.group());
}

private static CreateAnchorRequest readCreateAnchorRequest(RegistryFriendlyByteBuf buf) {
    return new CreateAnchorRequest(
            buf.readUtf(tech.endorsed.signport.world.AnchorCreation.MAX_ANCHOR_NAME_LENGTH),
            buf.readUtf());
}

private static void writeCreateAnchorResponse(RegistryFriendlyByteBuf buf, CreateAnchorResponse payload) {
    buf.writeBoolean(payload.success());
    buf.writeUtf(payload.errorMessage() == null ? "" : payload.errorMessage());
}

private static CreateAnchorResponse readCreateAnchorResponse(RegistryFriendlyByteBuf buf) {
    return new CreateAnchorResponse(buf.readBoolean(), buf.readUtf());
}
```

Update registration:

```java
PayloadTypeRegistry.clientboundPlay().register(CREATE_ANCHOR_RESPONSE_TYPE, CREATE_ANCHOR_RESPONSE_CODEC);
PayloadTypeRegistry.serverboundPlay().register(CREATE_ANCHOR_REQUEST_TYPE, CREATE_ANCHOR_REQUEST_CODEC);
```

- [ ] **Step 4: Run payload tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests tech.endorsed.signport.network.AnchorSyncPayloadsTest > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED|cannot find symbol|incompatible types' | Select-Object -First 80
```

Expected: no output from `Select-String`.

- [ ] **Step 5: Commit**

Run:

```powershell
git add src/main/java/tech/endorsed/signport/network/AnchorSyncPayloads.java src/test/java/tech/endorsed/signport/network/AnchorSyncPayloadsTest.java
git commit -m "Add anchor creation payloads"
```

Expected: commit succeeds.

---

### Task 4: Handle Create Requests On The Server

**Files:**
- Modify: `src/main/java/tech/endorsed/signport/network/AnchorSyncServer.java`

- [ ] **Step 1: Register the create request receiver**

In `AnchorSyncServer.register()`, after the ready receiver, add:

```java
ServerPlayNetworking.registerGlobalReceiver(AnchorSyncPayloads.CREATE_ANCHOR_REQUEST_TYPE, (payload, context) ->
        context.server().execute(() -> handleCreateAnchor(payload, context.player())));
```

- [ ] **Step 2: Add server-side handler**

Add imports:

```java
import net.minecraft.core.BlockPos;
import tech.endorsed.signport.bluemap.BlueMapIntegration;
import tech.endorsed.signport.world.AnchorCreation;
```

Add method:

```java
private static void handleCreateAnchor(AnchorSyncPayloads.CreateAnchorRequest payload, ServerPlayer player) {
    if (!ServerPlayNetworking.canSend(player, AnchorSyncPayloads.CREATE_ANCHOR_RESPONSE_TYPE)) return;

    var source = player.createCommandSourceStack();
    if (!SignPortPermissions.canCreateAnchor(source)) {
        ServerPlayNetworking.send(player, AnchorSyncPayloads.CreateAnchorResponse.failure("You do not have permission to create anchors"));
        return;
    }

    AnchorState state = AnchorState.getServerState(player.level().getServer());
    BlockPos pos = player.blockPosition();
    AnchorCreation.Result result = AnchorCreation.create(state, payload.name(), pos, player.level().dimension(), payload.group());
    if (!result.success()) {
        ServerPlayNetworking.send(player, AnchorSyncPayloads.CreateAnchorResponse.failure(result.errorMessage()));
        return;
    }

    Anchor anchor = result.anchor();
    BlueMapIntegration.anchorCreated(player.level().getServer(), anchor);
    anchorCreated(player.level().getServer(), anchor);
    ServerPlayNetworking.send(player, AnchorSyncPayloads.CreateAnchorResponse.success());
}
```

- [ ] **Step 3: Compile main sources**

Run:

```powershell
.\gradlew.bat --quiet compileJava > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED|cannot find symbol|incompatible types' | Select-Object -First 80
```

Expected: no output from `Select-String`.

- [ ] **Step 4: Commit**

Run:

```powershell
git add src/main/java/tech/endorsed/signport/network/AnchorSyncServer.java
git commit -m "Handle anchor creation requests"
```

Expected: commit succeeds.

---

### Task 5: Wire Client Response Handling

**Files:**
- Modify: `src/client/java/tech/endorsed/signport/client/SignPortClient.java`
- Modify: `src/client/java/tech/endorsed/signport/client/gui/AnchorBrowserScreen.java`

- [ ] **Step 1: Add a current-screen response method**

In `AnchorBrowserScreen`, add this public method:

```java
public void handleCreateAnchorResponse(AnchorSyncPayloads.CreateAnchorResponse response) {
    createPending = false;
    if (response.success()) {
        closeCreateDialog();
        rebuildRows();
        return;
    }
    createStatusMessage = response.errorMessage();
    createServerRejected = true;
    updateCreateValidation();
}
```

This method references fields and helpers added in Task 6. If compiling this task before Task 6, add it in Task 6 instead.

- [ ] **Step 2: Register response receiver**

In `SignPortClient.onInitializeClient()`, after the delta receiver, add:

```java
ClientPlayNetworking.registerGlobalReceiver(AnchorSyncPayloads.CREATE_ANCHOR_RESPONSE_TYPE, (payload, context) ->
        context.client().execute(() -> {
            if (context.client().screen instanceof AnchorBrowserScreen browser) {
                browser.handleCreateAnchorResponse(payload);
            }
        }));
```

- [ ] **Step 3: Compile client sources after Task 6**

Run this after Task 6 fields/helpers exist:

```powershell
.\gradlew.bat --quiet compileClientJava > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED|cannot find symbol|incompatible types' | Select-Object -First 80
```

Expected: no output from `Select-String`.

Do not commit Task 5 until Task 6 compiles with it.

---

### Task 6: Add Anchor Browser Create Modal

**Files:**
- Modify: `src/client/java/tech/endorsed/signport/client/gui/AnchorBrowserScreen.java`

- [ ] **Step 1: Add modal state fields**

Add imports:

```java
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import tech.endorsed.signport.network.AnchorSyncPayloads;
import tech.endorsed.signport.world.AnchorCreation;
import java.util.LinkedHashSet;
import java.util.Set;
```

Add fields near the existing browser fields:

```java
private Button createButton;
private EditBox createNameField;
private EditBox createGroupField;
private boolean createDialogOpen = false;
private boolean createPending = false;
private boolean createServerRejected = false;
private String createStatusMessage = "";
private ValidationState createValidation = ValidationState.RED;
private List<String> createGroupSuggestions = List.of();
private int selectedGroupSuggestion = 0;
```

Add enum near `SortMode`:

```java
private enum ValidationState {
    GREEN(0xFF55FF55),
    ORANGE(0xFFFFAA00),
    RED(0xFFFF5555);

    private final int color;

    ValidationState(int color) {
        this.color = color;
    }
}
```

- [ ] **Step 2: Add Create button in `init()`**

Change the search field width to leave room for Create, and add:

```java
if (canCreateAnchor()) {
    createButton = this.addRenderableWidget(Button.builder(Component.literal("Create"), button -> openCreateDialog())
            .bounds(left + PANEL_WIDTH - 172, top + 6, 64, 20)
            .build());
}
```

Keep the existing Done button at the top right.

Add helper:

```java
private boolean canCreateAnchor() {
    return SignPortClientState.serverHasSignPort() && SignPortClientState.permissions().canCreateAnchor();
}
```

- [ ] **Step 3: Add modal open/close helpers**

Add:

```java
private void openCreateDialog() {
    if (!canCreateAnchor()) return;
    createDialogOpen = true;
    createPending = false;
    createServerRejected = false;
    createStatusMessage = "";
    int left = left();
    int top = top();
    createNameField = new EditBox(this.font, left + 92, top + 88, 150, 18, Component.literal("Anchor name"));
    createNameField.setMaxLength(AnchorCreation.MAX_ANCHOR_NAME_LENGTH);
    createNameField.setResponder(ignored -> updateCreateValidation());
    createGroupField = new EditBox(this.font, left + 92, top + 116, 150, 18, Component.literal("Group"));
    createGroupField.setResponder(ignored -> updateCreateValidation());
    updateCreateValidation();
    setInitialFocus(createNameField);
}

private void closeCreateDialog() {
    createDialogOpen = false;
    createPending = false;
    createServerRejected = false;
    createStatusMessage = "";
    createNameField = null;
    createGroupField = null;
}
```

- [ ] **Step 4: Add validation helpers**

Add:

```java
private void updateCreateValidation() {
    String name = createNameField == null ? "" : createNameField.getValue().trim();
    createGroupSuggestions = groupSuggestions(createGroupField == null ? "" : createGroupField.getValue());
    selectedGroupSuggestion = Math.min(selectedGroupSuggestion, Math.max(0, createGroupSuggestions.size() - 1));

    if (createServerRejected) {
        createValidation = ValidationState.RED;
        return;
    }
    if (name.isEmpty() || name.length() > AnchorCreation.MAX_ANCHOR_NAME_LENGTH || selectedDimension == null) {
        createValidation = ValidationState.RED;
        createStatusMessage = "Enter an anchor name";
        return;
    }
    if (SignPortClientState.find(name, selectedDimension).isPresent()) {
        createValidation = ValidationState.RED;
        createStatusMessage = "Name already exists in this dimension";
        return;
    }
    if (SignPortClientState.findAnyDimension(name).isPresent()) {
        createValidation = ValidationState.ORANGE;
        createStatusMessage = "Name exists in another dimension. Signs may need a dimension line.";
        return;
    }
    createValidation = ValidationState.GREEN;
    createStatusMessage = "Ready to create";
}

private List<String> groupSuggestions(String input) {
    String needle = input == null ? "" : input.toLowerCase(Locale.ROOT);
    Set<String> groups = new LinkedHashSet<>();
    for (AnchorClient anchor : SignPortClientState.anchors()) {
        if (anchor.group() != null && !anchor.group().isBlank()
                && anchor.group().toLowerCase(Locale.ROOT).contains(needle)) {
            groups.add(anchor.group());
        }
    }
    return groups.stream().limit(5).toList();
}
```

- [ ] **Step 5: Add submit helper**

Add:

```java
private void submitCreateDialog() {
    updateCreateValidation();
    if (createValidation == ValidationState.RED || createPending || createNameField == null) return;
    createPending = true;
    createServerRejected = false;
    createStatusMessage = "Creating...";
    ClientPlayNetworking.send(new AnchorSyncPayloads.CreateAnchorRequest(
            createNameField.getValue().trim(),
            createGroupField == null ? "" : createGroupField.getValue()));
}
```

- [ ] **Step 6: Render modal**

At the end of `render(...)`, after the browser rows render, call:

```java
if (createDialogOpen) {
    renderCreateDialog(graphics);
}
```

Add:

```java
private void renderCreateDialog(GuiGraphics graphics) {
    int left = left() + 58;
    int top = top() + 64;
    int width = 264;
    int height = 128;
    graphics.fill(left, top, left + width, top + height, 0xEE111111);
    graphics.outline(left, top, width, height, 0xFF7DA7D9);
    graphics.drawString(this.font, "Create anchor here", left + 10, top + 10, 0xFFFFFFFF);
    graphics.drawString(this.font, "Name", left + 10, top + 28, 0xFFAAAAAA);
    graphics.drawString(this.font, "Group", left + 10, top + 56, 0xFFAAAAAA);
    graphics.drawString(this.font, liveLocationLabel(), left + 10, top + 84, 0xFFAAAAAA);
    graphics.drawString(this.font, createStatusMessage, left + 10, top + 98, createValidation.color);
}

private String liveLocationLabel() {
    if (this.minecraft == null || this.minecraft.player == null || this.minecraft.level == null) {
        return "Current location unavailable";
    }
    BlockPos pos = this.minecraft.player.blockPosition();
    return "%s %d %d %d".formatted(
            this.minecraft.level.dimension().identifier(),
            pos.getX(), pos.getY(), pos.getZ());
}
```

Also render `createNameField` and `createGroupField` from `render(...)` when the modal is open:

```java
if (createDialogOpen) {
    createNameField.render(graphics, mouseX, mouseY, partialTick);
    createGroupField.render(graphics, mouseX, mouseY, partialTick);
}
```

- [ ] **Step 7: Handle modal clicks and keys**

At the top of `mouseClicked(...)`, if `createDialogOpen`, route clicks to the modal fields/buttons and return true for modal area clicks.

Use this behavior:

```java
if (createDialogOpen) {
    if (createNameField.mouseClicked(event, doubleClick) || createGroupField.mouseClicked(event, doubleClick)) return true;
    return true;
}
```

At the top of `keyPressed(...)`, add:

```java
if (createDialogOpen) {
    if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
        closeCreateDialog();
        return true;
    }
    if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
        submitCreateDialog();
        return true;
    }
    return createNameField.keyPressed(event) || createGroupField.keyPressed(event) || super.keyPressed(event);
}
```

If the local event API uses accessor methods different from `event.key()`, inspect nearby code in `AbstractSignEditScreenMixin` and use the same `KeyEvent` access pattern.

- [ ] **Step 8: Add response method from Task 5**

Ensure this method exists:

```java
public void handleCreateAnchorResponse(AnchorSyncPayloads.CreateAnchorResponse response) {
    createPending = false;
    if (response.success()) {
        closeCreateDialog();
        rebuildRows();
        return;
    }
    createStatusMessage = response.errorMessage();
    createServerRejected = true;
    updateCreateValidation();
}
```

- [ ] **Step 9: Compile client sources**

Run:

```powershell
.\gradlew.bat --quiet compileClientJava > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED|cannot find symbol|incompatible types|mixin apply failed' | Select-Object -First 80
```

Expected: no output from `Select-String`.

- [ ] **Step 10: Commit Task 5 and Task 6 together**

Run:

```powershell
git add src/client/java/tech/endorsed/signport/client/SignPortClient.java src/client/java/tech/endorsed/signport/client/gui/AnchorBrowserScreen.java
git commit -m "Add anchor browser create modal"
```

Expected: commit succeeds.

---

### Task 7: Add Focused Client Validation Tests If Feasible

**Files:**
- Prefer modifying existing client-safe tests if they can reference helper methods.
- If `AnchorBrowserScreen` validation remains private and hard to instantiate, do not create brittle Minecraft UI tests. Cover the pure validation rules by extracting a package-private helper instead.

- [ ] **Step 1: Decide whether to extract a validation helper**

If `AnchorBrowserScreen` validation cannot be tested without Minecraft client runtime, create:

`src/client/java/tech/endorsed/signport/client/gui/AnchorCreateValidation.java`

with:

```java
package tech.endorsed.signport.client.gui;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import tech.endorsed.signport.client.SignPortClientState;
import tech.endorsed.signport.world.AnchorCreation;

final class AnchorCreateValidation {
    private AnchorCreateValidation() {
    }

    static State validate(String name, ResourceKey<Level> currentDimension) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty() || normalized.length() > AnchorCreation.MAX_ANCHOR_NAME_LENGTH || currentDimension == null) {
            return State.RED;
        }
        if (SignPortClientState.find(normalized, currentDimension).isPresent()) {
            return State.RED;
        }
        if (SignPortClientState.findAnyDimension(normalized).isPresent()) {
            return State.ORANGE;
        }
        return State.GREEN;
    }

    enum State {
        GREEN,
        ORANGE,
        RED
    }
}
```

Then have `AnchorBrowserScreen` use this helper to set its display colors.

- [ ] **Step 2: Add tests only if helper extraction stays lightweight**

Create `src/test/java/tech/endorsed/signport/client/gui/AnchorCreateValidationTest.java` only if test source can access the client package without client runtime failures. Add tests for red blank, red current-dimension duplicate, orange other-dimension duplicate, and green unused name.

- [ ] **Step 3: Run targeted tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests tech.endorsed.signport.client.gui.AnchorCreateValidationTest > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED|cannot find symbol|incompatible types' | Select-Object -First 80
```

Expected: no output from `Select-String`. If this fails because client classes are not available to tests, remove the brittle test/helper split and rely on `AnchorCreationTest` plus payload tests.

- [ ] **Step 4: Commit if tests/helper were added**

Run:

```powershell
git add src/client/java/tech/endorsed/signport/client/gui/AnchorCreateValidation.java src/client/java/tech/endorsed/signport/client/gui/AnchorBrowserScreen.java src/test/java/tech/endorsed/signport/client/gui/AnchorCreateValidationTest.java
git commit -m "Test anchor create validation states"
```

Expected: commit succeeds if files exist. Skip this commit if no helper/test was added.

---

### Task 8: Full Verification And Cleanup

**Files:**
- Review all changed files.

- [ ] **Step 1: Run full quiet build**

Run:

```powershell
.\gradlew.bat --quiet build > build_out.txt 2>&1
Select-String -Path build_out.txt -Pattern 'error:|FAILED|cannot find symbol|incompatible types|mixin apply failed' | Select-Object -First 80
```

Expected: no output from `Select-String`.

- [ ] **Step 2: Review diff summary**

Run:

```powershell
git diff --stat
git status --short
```

Expected: only intended files changed, or clean if every task was committed.

- [ ] **Step 3: Inspect final behavior-critical diffs**

Run:

```powershell
git diff HEAD~6 -- src/main/java/tech/endorsed/signport/network/AnchorSyncPayloads.java src/main/java/tech/endorsed/signport/network/AnchorSyncServer.java src/client/java/tech/endorsed/signport/client/gui/AnchorBrowserScreen.java
```

Expected:

- Request payload contains only name and group.
- Response payload contains success and error text.
- Server derives position and dimension from `ServerPlayer`.
- Modal uses response payload for close/error behavior.
- No Java code reads position or dimension from the client request.

- [ ] **Step 4: Final commit if needed**

If any verification-only fixes remain uncommitted, run:

```powershell
git add <changed-files>
git commit -m "Polish anchor creation GUI"
```

Expected: commit succeeds, or no commit needed.

---

## Plan Self-Review

- Spec coverage:
  - Client-only GUI: Task 6 only exposes browser/modal in client source.
  - Existing keybind entry point: Task 6 adds Create button to existing browser, no new keybind.
  - Dedicated request/response payloads: Task 3.
  - Server-derived position/dimension: Task 4.
  - Shared command-equivalent creation behavior: Task 2 and Task 4.
  - Modal pending/success/rejection behavior: Task 5 and Task 6.
  - Green/orange/red validation: Task 6 and optional Task 7.
  - Group suggestions as-is: Task 6.
  - Named max-length constant visible to command/network: Task 2 and Task 3.
  - Changelog: Task 1.
- Placeholder scan:
  - The only conditional path is Task 7, which is explicit about when to add or skip brittle client tests.
- Type consistency:
  - `AnchorCreation.Result`, `AnchorCreation.Error`, `CreateAnchorRequest`, and `CreateAnchorResponse` names are introduced before later tasks use them.
