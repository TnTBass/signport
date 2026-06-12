package tech.endorsed.signport.permission;

import net.minecraft.server.permissions.PermissionSet;
import org.junit.jupiter.api.Test;
import tech.endorsed.signport.config.SignPortConfig;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignPortPermissionPolicyTest {
    @Test
    void commonPolicyDefinesStablePermissionNodesAndDefaultSources() {
        Map<String, SignPortPermissionPolicy.DefaultSource> defaultsByNode =
                SignPortPermissionPolicy.defaultsByNode();

        assertEquals(SignPortPermissionPolicy.DefaultSource.PROTECTED_ACTION_OP_LEVEL,
                defaultsByNode.get("signport.anchor.create"));
        assertEquals(SignPortPermissionPolicy.DefaultSource.PROTECTED_ACTION_OP_LEVEL,
                defaultsByNode.get("signport.anchor.delete"));
        assertEquals(SignPortPermissionPolicy.DefaultSource.PROTECTED_ACTION_OP_LEVEL,
                defaultsByNode.get("signport.anchor.list"));
        assertEquals(SignPortPermissionPolicy.DefaultSource.TELEPORT_COMMAND_DEFAULT,
                defaultsByNode.get("signport.teleport.command"));
        assertEquals(SignPortPermissionPolicy.DefaultSource.PROTECTED_ACTION_OP_LEVEL,
                defaultsByNode.get("signport.sign.create"));
        assertEquals(SignPortPermissionPolicy.DefaultSource.PROTECTED_ACTION_OP_LEVEL,
                defaultsByNode.get("signport.sign.edit"));
        assertEquals(SignPortPermissionPolicy.DefaultSource.PROTECTED_ACTION_OP_LEVEL,
                defaultsByNode.get("signport.sign.break"));
        assertEquals(SignPortPermissionPolicy.DefaultSource.SIGN_USE_DEFAULT,
                defaultsByNode.get("signport.sign.use"));
        assertEquals(8, defaultsByNode.size());
    }

    @Test
    void vanillaOpFallbackTreatsAllPermissionsAsAllowedForNonPlayerSources() {
        assertTrue(invokeHasOpLevel(PermissionSet.ALL_PERMISSIONS, 4));
    }

    @Test
    void vanillaOpFallbackDoesNotTreatEmptyPermissionsAsAllowedForNonPlayerSources() {
        assertFalse(invokeHasOpLevel(PermissionSet.NO_PERMISSIONS, 0));
    }

    @Test
    void defaultSourcesResolveAgainstConfigValues() {
        SignPortConfig.Values config = new SignPortConfig.Values(false, true, 3, true, true, 10, 128, true);

        assertEquals(SignPortPermissionPolicy.PermissionDefault.opLevel(3),
                SignPortPermissionPolicy.DefaultSource.PROTECTED_ACTION_OP_LEVEL.resolve(config));
        assertEquals(SignPortPermissionPolicy.PermissionDefault.fallback(false),
                SignPortPermissionPolicy.DefaultSource.TELEPORT_COMMAND_DEFAULT.resolve(config));
        assertEquals(SignPortPermissionPolicy.PermissionDefault.fallback(true),
                SignPortPermissionPolicy.DefaultSource.SIGN_USE_DEFAULT.resolve(config));
    }

    private static boolean invokeHasOpLevel(PermissionSet permissions, int opLevel) {
        try {
            Method method = SignPortPermissions.class.getDeclaredMethod("hasOpLevel", PermissionSet.class, int.class);
            method.setAccessible(true);
            return (boolean) method.invoke(null, permissions, opLevel);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
