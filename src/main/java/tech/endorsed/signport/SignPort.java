package tech.endorsed.signport;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.endorsed.signport.command.AnchorCommand;
import tech.endorsed.signport.config.SignPortConfig;
import tech.endorsed.signport.events.SignEvents;
import tech.endorsed.signport.world.AnchorState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SignPort implements ModInitializer {
	public static String MOD_ID = "signport";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		SignPortConfig.load();

		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, world) -> {
			AnchorCommand.register(dispatcher);
		});

		PlayerBlockBreakEvents.BEFORE.register(new SignEvents());

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			// Eagerly load state so legacy file migration runs at startup
			// rather than silently on the first write operation.
			AnchorState state = AnchorState.getServerState(server);
			int total = state.anchors.size();
			if (total == 0) {
				LOGGER.info("[SignPort] No anchors loaded.");
			} else {
				Map<String, Long> byDimension = state.anchors.stream().collect(
						Collectors.groupingBy(
								a -> a.dimension.location().getPath(),
								LinkedHashMap::new,
								Collectors.counting()));
				String summary = byDimension.entrySet().stream()
						.map(e -> e.getValue() + " in " + e.getKey())
						.collect(Collectors.joining(", "));
				LOGGER.info("[SignPort] Loaded {} anchor(s): {}.", total, summary);
			}
		});
	}
}
