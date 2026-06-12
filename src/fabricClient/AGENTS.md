# Fabric Client Source Root

This root owns Fabric client-side adapter code.

- Fabric client entrypoints, keybind/tick/connect/disconnect hooks, Fabric client networking, ModMenu entrypoints, and Fabric client metadata/config path lookup belong here.
- Keep reusable client state, layout, and validation in `src/commonClient`.
- Do not add NeoForge client adapter code or NeoForge resources here.
