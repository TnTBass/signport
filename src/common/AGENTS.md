# Common Source Root

This root owns loader-neutral SignPort behavior only.

- Do not import Fabric, ModMenu, Fabric Permissions API, NeoForge, BlueMap API, LuckPerms, or provider-specific permission APIs here.
- Keep Minecraft gameplay/domain code here when it can compile and run without loader lifecycle, transport, metadata, or provider APIs.
- Use small common facades for cross-loader notifications; loader adapters install the runtime implementations.
