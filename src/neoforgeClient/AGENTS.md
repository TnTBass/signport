# NeoForge Client Source Root

This root owns NeoForge client-side adapter code.

- NeoForge client entrypoints, keybind/tick/connect/disconnect hooks, NeoForge client networking, and NeoForge client metadata/config path lookup belong here.
- Keep reusable client state, layout, and validation in `src/commonClient`.
- Do not add Fabric client adapter code, ModMenu entrypoints, or Fabric resources here.
