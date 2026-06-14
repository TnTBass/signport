package tech.endorsed.signport.neoforge.network;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeNetworkingBoundaryTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir")).getParent();

    @Test
    void neoforgeServerRegistersStablePayloadContracts() throws Exception {
        String source = Files.readString(PROJECT_ROOT.resolve(
                "src/neoforge/java/tech/endorsed/signport/neoforge/network/NeoForgePayloads.java"));

        assertAll(
                () -> assertTrue(source.contains("event.registrar(SignPort.MOD_ID).optional()")),
                () -> assertTrue(source.contains("AnchorSyncPayloads.FULL_TYPE")),
                () -> assertTrue(source.contains("AnchorSyncPayloads.DELTA_TYPE")),
                () -> assertTrue(source.contains("AnchorSyncPayloads.READY_TYPE")),
                () -> assertTrue(source.contains("AnchorSyncPayloads.CREATE_ANCHOR_REQUEST_TYPE")),
                () -> assertTrue(source.contains("AnchorSyncPayloads.CREATE_ANCHOR_RESPONSE_TYPE")),
                () -> assertTrue(source.contains("StatusPayloads.VERSION_TYPE")),
                () -> assertTrue(source.contains("StatusPayloads.REQUEST_TYPE")));
    }

    @Test
    void neoforgeEntrypointInstallsRealAnchorSyncAndStatusTransport() throws Exception {
        String source = Files.readString(PROJECT_ROOT.resolve(
                "src/neoforge/java/tech/endorsed/signport/neoforge/SignPortNeoForge.java"));
        String anchorSyncSource = Files.readString(PROJECT_ROOT.resolve(
                "src/neoforge/java/tech/endorsed/signport/neoforge/network/NeoForgeAnchorSyncServer.java"));
        String statusSource = Files.readString(PROJECT_ROOT.resolve(
                "src/neoforge/java/tech/endorsed/signport/neoforge/network/NeoForgeSignPortStatusNetworking.java"));

        assertAll(
                () -> assertFalse(source.contains("AnchorSyncServer.Adapter.NO_OP")),
                () -> assertTrue(source.contains("SignPortNeoForge(IEventBus modBus)")),
                () -> assertTrue(source.contains("NeoForgeAnchorSyncServer.register()")),
                () -> assertTrue(source.contains("NeoForgeSignPortStatusNetworking.register()")),
                () -> assertTrue(source.contains("NeoForgePayloads::register")),
                () -> assertTrue(anchorSyncSource.contains("NeoForge.EVENT_BUS.addListener(NeoForgeAnchorSyncServer::onPlayerRespawn)")),
                () -> assertTrue(statusSource.contains("NeoForge.EVENT_BUS.addListener(NeoForgeSignPortStatusNetworking::onPlayerLoggedIn)")));
    }

    @Test
    void neoforgeServerSendsAreCapabilityGated() throws Exception {
        String anchorSyncSource = Files.readString(PROJECT_ROOT.resolve(
                "src/neoforge/java/tech/endorsed/signport/neoforge/network/NeoForgeAnchorSyncServer.java"));
        String statusSource = Files.readString(PROJECT_ROOT.resolve(
                "src/neoforge/java/tech/endorsed/signport/neoforge/network/NeoForgeSignPortStatusNetworking.java"));

        assertAll(
                () -> assertTrue(anchorSyncSource.contains("canSend(player, AnchorSyncPayloads.FULL_ID)")),
                () -> assertTrue(anchorSyncSource.contains("canSend(player, AnchorSyncPayloads.DELTA_ID)")),
                () -> assertTrue(anchorSyncSource.contains("canSend(player, AnchorSyncPayloads.CREATE_ANCHOR_RESPONSE_ID)")),
                () -> assertTrue(anchorSyncSource.contains("((ICommonPacketListener) player.connection).hasChannel(channel)")),
                () -> assertTrue(statusSource.contains("((ICommonPacketListener) player.connection).hasChannel(StatusPayloads.VERSION_ID)")));
    }

    @Test
    void neoforgeClientWiresAnchorSyncAndStatusRuntime() throws Exception {
        String source = Files.readString(PROJECT_ROOT.resolve(
                "src/neoforgeClient/java/tech/endorsed/signport/neoforge/client/SignPortNeoForgeClient.java"));
        String buildGradle = Files.readString(PROJECT_ROOT.resolve("neoforge/build.gradle"));

        assertAll(
                () -> assertTrue(buildGradle.contains("../src/commonClient/java")),
                () -> assertTrue(buildGradle.contains("compileOnly files({ rootProject.configurations.clientCompileClasspath.files })")),
                () -> assertTrue(buildGradle.contains("testImplementation files({ rootProject.configurations.clientCompileClasspath.files })")),
                () -> assertTrue(source.contains("RegisterClientPayloadHandlersEvent")),
                () -> assertTrue(source.contains("event.register(AnchorSyncPayloads.FULL_TYPE")),
                () -> assertTrue(source.contains("event.register(AnchorSyncPayloads.DELTA_TYPE")),
                () -> assertTrue(source.contains("event.register(AnchorSyncPayloads.CREATE_ANCHOR_RESPONSE_TYPE")),
                () -> assertTrue(source.contains("event.register(StatusPayloads.VERSION_TYPE")),
                () -> assertTrue(source.contains("AnchorBrowserScreen.installCreateAnchorSender(ClientPacketDistributor::sendToServer)")),
                () -> assertTrue(source.contains("ClientPacketDistributor.sendToServer(StatusPayloads.ServerVersionRequest.INSTANCE)")),
                () -> assertTrue(source.contains("ClientPacketDistributor.sendToServer(new AnchorSyncPayloads.Ready())")),
                () -> assertTrue(source.contains("SignPortStatusNetworking.shouldRequestServerVersion")),
                () -> assertTrue(source.contains("AnchorSyncPayloads.shouldRequestInitialSync")),
                () -> assertTrue(source.contains("private static void onClientLoggingIn")),
                () -> assertTrue(source.contains("SignPortStatus.onClientDisconnect()")),
                () -> assertTrue(source.contains("SignPortClientState.clear()")),
                () -> assertTrue(source.contains("ClientPacketListener listener = client.getConnection()")),
                () -> assertTrue(source.contains("((ICommonPacketListener) listener).hasChannel(StatusPayloads.REQUEST_ID)")),
                () -> assertTrue(source.contains("((ICommonPacketListener) listener).hasChannel(AnchorSyncPayloads.READY_ID)")),
                () -> assertFalse(source.contains("import net.minecraft.resources.Identifier")),
                () -> assertFalse(source.contains("net.fabricmc")));
    }
}
