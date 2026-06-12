package tech.endorsed.signport.permission;

import tech.endorsed.signport.config.SignPortConfig;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SignPortPermissionPolicy {
    public static final String ANCHOR_CREATE = "signport.anchor.create";
    public static final String ANCHOR_DELETE = "signport.anchor.delete";
    public static final String ANCHOR_LIST = "signport.anchor.list";
    public static final String TELEPORT_COMMAND = "signport.teleport.command";
    public static final String SIGN_CREATE = "signport.sign.create";
    public static final String SIGN_EDIT = "signport.sign.edit";
    public static final String SIGN_BREAK = "signport.sign.break";
    public static final String SIGN_USE = "signport.sign.use";

    private static final Map<String, DefaultSource> DEFAULTS_BY_NODE = createDefaultsByNode();

    private SignPortPermissionPolicy() {
    }

    public static Map<String, DefaultSource> defaultsByNode() {
        return DEFAULTS_BY_NODE;
    }

    private static Map<String, DefaultSource> createDefaultsByNode() {
        Map<String, DefaultSource> defaults = new LinkedHashMap<>();
        defaults.put(ANCHOR_CREATE, DefaultSource.PROTECTED_ACTION_OP_LEVEL);
        defaults.put(ANCHOR_DELETE, DefaultSource.PROTECTED_ACTION_OP_LEVEL);
        defaults.put(ANCHOR_LIST, DefaultSource.PROTECTED_ACTION_OP_LEVEL);
        defaults.put(TELEPORT_COMMAND, DefaultSource.TELEPORT_COMMAND_DEFAULT);
        defaults.put(SIGN_CREATE, DefaultSource.PROTECTED_ACTION_OP_LEVEL);
        defaults.put(SIGN_EDIT, DefaultSource.PROTECTED_ACTION_OP_LEVEL);
        defaults.put(SIGN_BREAK, DefaultSource.PROTECTED_ACTION_OP_LEVEL);
        defaults.put(SIGN_USE, DefaultSource.SIGN_USE_DEFAULT);
        return Map.copyOf(defaults);
    }

    public enum DefaultSource {
        PROTECTED_ACTION_OP_LEVEL {
            @Override
            public PermissionDefault resolve(SignPortConfig.Values config) {
                return PermissionDefault.opLevel(config.protectedActionOpLevel());
            }
        },
        TELEPORT_COMMAND_DEFAULT {
            @Override
            public PermissionDefault resolve(SignPortConfig.Values config) {
                return PermissionDefault.fallback(config.teleportCommandDefault());
            }
        },
        SIGN_USE_DEFAULT {
            @Override
            public PermissionDefault resolve(SignPortConfig.Values config) {
                return PermissionDefault.fallback(config.signUseDefault());
            }
        };

        public abstract PermissionDefault resolve(SignPortConfig.Values config);
    }

    public record PermissionDefault(Integer opLevel, Boolean fallback) {
        public static PermissionDefault opLevel(int opLevel) {
            return new PermissionDefault(opLevel, null);
        }

        public static PermissionDefault fallback(boolean fallback) {
            return new PermissionDefault(null, fallback);
        }
    }
}
