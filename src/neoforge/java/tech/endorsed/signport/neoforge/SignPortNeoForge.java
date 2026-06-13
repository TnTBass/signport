package tech.endorsed.signport.neoforge;

import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import tech.endorsed.signport.bluemap.BlueMapIntegration;
import tech.endorsed.signport.command.AnchorCommand;
import tech.endorsed.signport.config.SignPortConfig;
import tech.endorsed.signport.network.AnchorSyncServer;
import tech.endorsed.signport.neoforge.events.NeoForgeSignEvents;
import tech.endorsed.signport.neoforge.permission.NeoForgeSignPortPermissions;
import tech.endorsed.signport.permission.SignPortPermissions;
import tech.endorsed.signport.SignPort;
import tech.endorsed.signport.status.SignPortStatus;
import tech.endorsed.signport.world.AnchorState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Mod(SignPort.MOD_ID)
public final class SignPortNeoForge {
    public SignPortNeoForge() {
        SignPortConfig.load(FMLPaths.CONFIGDIR.get().resolve(SignPortConfig.FILE_NAME), SignPort.LOGGER);
        SignPortStatus.installVersionSupplier(SignPortNeoForge::resolveVersion);
        SignPortPermissions.install(new NeoForgeSignPortPermissions());
        BlueMapIntegration.install(BlueMapIntegration.Adapter.NO_OP);
        AnchorSyncServer.install(AnchorSyncServer.Adapter.NO_OP);

        NeoForgeSignEvents.register();
        NeoForge.EVENT_BUS.addListener(NeoForgeSignPortPermissions::registerNodes);
        NeoForge.EVENT_BUS.addListener(SignPortNeoForge::registerCommands);
        NeoForge.EVENT_BUS.addListener(SignPortNeoForge::serverStarted);
        NeoForge.EVENT_BUS.addListener(SignPortNeoForge::serverStopping);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        AnchorCommand.register(event.getDispatcher());
    }

    private static void serverStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        AnchorState state = AnchorState.getServerState(server);
        int total = state.anchors.size();
        if (total == 0) {
            SignPort.LOGGER.info("[SignPort] No anchors loaded.");
        } else {
            Map<String, Long> byDimension = state.anchors.stream().collect(
                    Collectors.groupingBy(
                            anchor -> anchor.dimension.identifier().getPath(),
                            LinkedHashMap::new,
                            Collectors.counting()));
            String summary = byDimension.entrySet().stream()
                    .map(entry -> entry.getValue() + " in " + entry.getKey())
                    .collect(Collectors.joining(", "));
            SignPort.LOGGER.info("[SignPort] Loaded {} anchor(s): {}.", total, summary);
        }
        BlueMapIntegration.serverStarted(server);
    }

    private static void serverStopping(ServerStoppingEvent event) {
        BlueMapIntegration.serverStopping(event.getServer());
    }

    private static String resolveVersion() {
        return ModList.get()
                .getModContainerById(SignPort.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("Unknown");
    }
}
