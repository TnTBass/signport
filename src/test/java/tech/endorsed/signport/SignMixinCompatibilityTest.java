package tech.endorsed.signport;

import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.junit.jupiter.api.Test;
import tech.endorsed.signport.mixin.SignBlockEntityMixin;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SignMixinCompatibilityTest {
    @Test
    void sharedSignProtectionCallbacksMatchTheMinecraftRuntime() {
        // Java compilation does not validate mixin injection descriptors. Both loaders
        // require these callbacks, so an API change here otherwise fails at startup.
        assertCallbackMatches("onTryChangeText", "updateSignText");
        assertCallbackMatches("onSignChange", "updateText");
    }

    private static void assertCallbackMatches(String callbackName, String targetName) {
        Method callback = Arrays.stream(SignBlockEntityMixin.class.getDeclaredMethods())
                .filter(method -> method.getName().equals(callbackName))
                .findFirst().orElseThrow();
        Class<?>[] parameters = callback.getParameterTypes();
        Class<?>[] targetParameters = Arrays.copyOf(parameters, parameters.length - 1);
        assertDoesNotThrow(() -> SignBlockEntity.class.getDeclaredMethod(targetName, targetParameters));
    }
}
