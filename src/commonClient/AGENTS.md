# Common Client Source Root

This root owns loader-neutral client state, layout, validation, and display behavior.

- Do not import Fabric, ModMenu, Fabric Permissions API, NeoForge, BlueMap API, LuckPerms, or provider-specific permission APIs here.
- Vanilla Minecraft client UI types are allowed when shared across loaders.
- Loader-specific networking, keybind registration, lifecycle hooks, metadata lookup, and optional integration entrypoints belong in loader client roots.
