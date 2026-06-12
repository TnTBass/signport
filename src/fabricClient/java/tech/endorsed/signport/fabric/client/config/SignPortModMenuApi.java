package tech.endorsed.signport.fabric.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class SignPortModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> tech.endorsed.signport.client.config.ConfigScreenFactory.isAvailable()
                ? tech.endorsed.signport.client.config.ConfigScreenFactory.create(parent)
                : parent;
    }
}
