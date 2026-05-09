package tech.endorsed.signport;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.endorsed.signport.command.AnchorCommand;
import tech.endorsed.signport.config.SignPortConfig;
import tech.endorsed.signport.events.SignEvents;

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
	}
}
