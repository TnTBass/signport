package tech.endorsed.signport.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import tech.endorsed.signport.bluemap.BlueMapIntegration;
import tech.endorsed.signport.command.AnchorCommand;
import tech.endorsed.signport.config.SignPortConfig;
import tech.endorsed.signport.fabric.bluemap.FabricBlueMapIntegration;
import tech.endorsed.signport.fabric.events.FabricSignEvents;
import tech.endorsed.signport.fabric.network.FabricAnchorSyncServer;
import tech.endorsed.signport.fabric.network.FabricSignPortStatusNetworking;
import tech.endorsed.signport.fabric.permission.FabricSignPortPermissions;
import tech.endorsed.signport.SignPort;
import tech.endorsed.signport.network.AnchorSyncServer;
import tech.endorsed.signport.permission.SignPortPermissions;
import tech.endorsed.signport.status.SignPortStatus;
import tech.endorsed.signport.world.AnchorState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SignPortFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		SignPortConfig.load(FabricLoader.getInstance().getConfigDir().resolve(SignPortConfig.FILE_NAME), SignPort.LOGGER);
		SignPortStatus.installVersionSupplier(SignPortFabric::resolveVersion);
		SignPortPermissions.install(new FabricSignPortPermissions());
		BlueMapIntegration.install(new FabricBlueMapIntegration());
		AnchorSyncServer.install(new AnchorSyncServer.Adapter() {
			@Override
			public void sendFullToAll(net.minecraft.server.MinecraftServer server) {
				FabricAnchorSyncServer.sendFullToAll(server);
			}

			@Override
			public void anchorCreated(net.minecraft.server.MinecraftServer server, tech.endorsed.signport.world.Anchor anchor) {
				FabricAnchorSyncServer.anchorCreated(server, anchor);
			}

			@Override
			public void anchorUpdated(net.minecraft.server.MinecraftServer server, tech.endorsed.signport.world.Anchor anchor) {
				FabricAnchorSyncServer.anchorUpdated(server, anchor);
			}

			@Override
			public void anchorDeleted(net.minecraft.server.MinecraftServer server, String name, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
				FabricAnchorSyncServer.anchorDeleted(server, name, dimension);
			}
		});
		BlueMapIntegration.initialize();
		FabricAnchorSyncServer.register();
		FabricSignPortStatusNetworking.register();

		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, world) -> {
			AnchorCommand.register(dispatcher);
		});

		PlayerBlockBreakEvents.BEFORE.register(new FabricSignEvents());

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			// Eagerly load state so legacy file migration runs at startup
			// rather than silently on the first write operation.
			AnchorState state = AnchorState.getServerState(server);
			int total = state.anchors.size();
			if (total == 0) {
				SignPort.LOGGER.info("[SignPort] No anchors loaded.");
			} else {
				Map<String, Long> byDimension = state.anchors.stream().collect(
						Collectors.groupingBy(
								a -> a.dimension.identifier().getPath(),
								LinkedHashMap::new,
								Collectors.counting()));
				String summary = byDimension.entrySet().stream()
						.map(e -> e.getValue() + " in " + e.getKey())
						.collect(Collectors.joining(", "));
				SignPort.LOGGER.info("[SignPort] Loaded {} anchor(s): {}.", total, summary);
			}
			BlueMapIntegration.serverStarted(server);
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(BlueMapIntegration::serverStopping);
	}

	private static String resolveVersion() {
		return FabricLoader.getInstance()
				.getModContainer(SignPort.MOD_ID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("Unknown");
	}
}
